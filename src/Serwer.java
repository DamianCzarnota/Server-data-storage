import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.File;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;



/**
 * Podstawowa aplikacja serwerowa służąca do komunikacji z klientami
 */
public class Serwer extends Application {
    TreeView<String> folders;
    TreeItem<String> root;

    /**
     *Funkcja Main uruchamiająca aplikacje serwera
     * @param args parametry przekazane do funkcji main
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     *Metoda uruchamiająca javaF oraz tworząca scenę główną serwera wyświetlająca zalogowanych
     * użytkowników na serwerze wraz z ich plikami.
     * @param primaryStage Główne okno aplikacji
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage = new Stage();
        primaryStage.setTitle("Serwer");
        Label label = new Label("Obecni zalogowani klienci");
        VBox layout = new VBox();
        root = new TreeItem<String>("root");
        root.setExpanded(true);
        folders = new TreeView<>(root);
        folders.setShowRoot(false);


        layout.getChildren().addAll(label,folders);
        Scene scene = new Scene(layout, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
        Thread t = new Listener();
        t.setDaemon(true);
        t.start();
    }

    /**
     *Wątek stale nasłuchujący przychodzących klientów i nawiązywujący połączenie między nimi.
     */
    class Listener extends Thread{
        @Override
        /**
         *Metoda wątku odpowiedzialna za prawidłowe funkcjonowanie serwara w kwestii przyjmowanie
         * przychodzących klientów
         */
        public void run(){
            try {
                ServerSocket s = new ServerSocket(5400);
                while (true) {
                    Socket cl = s.accept();
                    DataInputStream dis = new DataInputStream(cl.getInputStream());
                    DataOutputStream dos = new DataOutputStream(cl.getOutputStream());

                    //nawiązać połączenie
                    String username = dis.readUTF();
                    ClientReader t = new ClientReader("Thread: "+username,dis,dos,cl,username);
                    t.setDaemon(true);
                    t.start();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     *Wątek odpowiedzialny za komunikacje serwera z pojedyńczym użytkownikiem
     */
    class ClientReader extends Thread{
        Socket s;
        DataInputStream reader;
        DataOutputStream writter;
        String user;
        TreeItem<String> tree;

        /**
         * Konstruktor odpowiedzialny za inicjalizowanie obiektów koniecznych do poprawnej komunikacji z użytkownikiem
         * @param name Nazwa nowego indywidualnego dla każdego klienta wątku
         * @param is Strumień wejściowy gniazda
         * @param os Strumien wyjściowy gniazda
         * @param s gniazdo
         * @param n Nazwa użytkownika
         */
        public ClientReader(String name,DataInputStream is,DataOutputStream os,Socket s,String n){
            super(name);
            this.s=s;
            reader=is;
            user=n;
            writter=os;
        }
        /**
         *Główna metoda wątku odpowiedzialna za komunikacje i synchronizacje z użytkownikiem
         */
        @Override
        public void run(){
            //inicjalizacja
            Path dir = Paths.get(Paths.get("").toAbsolutePath().toString()+java.io.File.separator+"users");
            try {
                if (!Files.exists(dir)) {
                    Files.createDirectory(dir);
                }
                dir=Paths.get(dir.toString()+File.separator+user);
                if (!Files.exists(dir)) {
                    Files.createDirectory(dir);
                }
            }catch (IOException e){
                e.printStackTrace();
            }
            tree = TreeFunctions.makeBranch(user,root);
            TreeFunctions a = new TreeFunctions(dir,tree);
            a.setDaemon(true);
            a.start();
            //
            while (true) {
                try {
                    String nazwa = reader.readUTF();
                    if(nazwa.equals("Client")){
                        String us = reader.readUTF();
                        Path pa = Paths.get(reader.readUTF());
                        System.out.println(pa.toString());
                        System.out.println(dir.getParent().toString() + File.separator + us);
                        if(Files.isDirectory(Paths.get(dir.getParent().toString()+File.separator+us))) {
                            Files.copy(
                                    pa,
                                    Paths.get(dir.getParent().toString() + File.separator + us+File.separator
                                    +pa.getFileName().toString()),
                                    StandardCopyOption.REPLACE_EXISTING
                            );
                        }
                        reader.readUTF();
                    }
                    FileSendingServer(dir, s.getInputStream(), s.getOutputStream());
                    TimeUnit.SECONDS.sleep(1);
                }
                catch (Exception e) {
                    //e.printStackTrace();
                    break;
                }
            }
            tree.setExpanded(false);
            root.getChildren().remove(tree);
            a.interrupt();
            //rozłączene
        }
    }

    /**
     *Metoda rekurencyjna odpowiedzialna za synchronizacje i przesyłanie plików między dwoma folderami znajdującymi
     * się na serwerze i na kliencie. Jest to metoda bliźniacza do funkcji FileSendingClient różniąca się jedynie
     * kolejnąścią wysyłania lub odbierania wiadomości lub plików
     * @param path Ścieżka do folderu który będzie synchronizowany z katalogiem u użytkownika
     * @param is Strumień wejściowy gniazdka klienta
     * @param os Strumień wyjściowy gniazdka klienta
     * @throws Exception
     */
    void FileSendingServer(Path path,InputStream is,OutputStream os) throws Exception{
        DataOutputStream dos = new DataOutputStream(os);
        DataInputStream dis = new DataInputStream(is);
        String notification=new String();
        Iterator<Path> todivide = Files.list(path).iterator();
        while(todivide.hasNext()){
            Path div = todivide.next();
            notification = notification.concat(div.getFileName().toString()+'\n');
        }
        //otrzymanie plików od serwera
        String[] receive = dis.readUTF().split("\n");
        String request=new String();
        //Wysłanie swoich plików serwerowi
        dos.writeUTF(notification);
        //sprawdznie brakujących plików
        for(int i=0;i<receive.length;++i){
            todivide = Files.list(path).iterator();
            boolean missing = true;
            while (todivide.hasNext()){
                if(receive[i].equals(todivide.next().getFileName().toString())){
                    missing=false;
                    break;
                }
            }
            if(missing)
                request=request.concat(receive[i]+'\n');
        }

        //wysłanie prośby o brakujące pliki
        dos.writeUTF(request);
        //odebranie prośby o brakujące pliki
        String[] toSend = dis.readUTF().split("\n");
        //System.out.println(toSend);
        //odbiór plików
        receive=request.split("\n");
        //int FILE_SIZE=1024*1024;
        for(int i=0;i<receive.length;++i){
            String odp = dis.readUTF();
            if(odp.equals("Directory")){
                Files.createDirectories(Paths.get(path.toString()+File.separator+receive[i]));
            }
            if(odp.equals("File")){
                Long size = dis.readLong();
                int sendSize = Integer.parseInt(size.toString());
                byte [] mybytearray  = new byte [sendSize];

                FileOutputStream fos = new FileOutputStream(Paths.get(path.toString()+File.separator+receive[i]).toFile());
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                //bytesRead = is.read(mybytearray,0,mybytearray.length);
                int bytesRead;
                while (size > 0 && (bytesRead = is.read(mybytearray, 0, (int)Math.min(mybytearray.length, size))) != -1)
                {
                    size -= bytesRead;
                }
                dos.writeUTF("");
                bos.write(mybytearray, 0 , sendSize);
                bos.flush();


                bos.close();
                fos.close();//może?
            }
        }
        //wysył plików
        for (int i=0;i<toSend.length;++i){
            if(Files.isDirectory(Paths.get(path.toString()+File.separator+toSend[i]))){
                dos.writeUTF("Directory");
            }else
            if(Files.exists(Paths.get(path.toString()+File.separator+toSend[i]))){
                dos.writeUTF("File");
                dos.writeLong(Files.size(Paths.get(path.toString()+File.separator+toSend[i])));
                File myFile = new File (path.toString()+File.separator+toSend[i]);
                byte [] mybytearray  = new byte [(int)myFile.length()];
                FileInputStream fis = new FileInputStream(myFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(mybytearray,0,mybytearray.length);
                os.write(mybytearray,0,mybytearray.length);
                dis.readUTF();
                os.flush();
                bis.close();
                fis.close();
            }
        }
        //zejsie nizej
        todivide = Files.list(path).iterator();
        while(todivide.hasNext()){
            Path div = todivide.next();
            if(Files.isDirectory(div)){
                FileSendingServer(div,is,os);
            }
        }
    }
}
