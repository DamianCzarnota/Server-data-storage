import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.*;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;


/**
 * Podstawowa aplikacja kliencka służąca do komunikacji z serwerem
 */
public class Klient extends Application{
    boolean secondStageBuilded;
    Stage stage;
    Scene log,conn;
    Socket s;
    Label label;
    Button but;
    DataInputStream dis;
    DataOutputStream dos;
    TreeView<String> tree;
    TreeItem<String> root;

    /**
     *Funkcja Main uruchamiająca aplikacje kliencką
     * @param args parametry przekazane do funkcji main
     */
    public static void main(String[] args){
        launch(args);
    }

    /**
     *Metoda uruchamiająca javaF oraz tworząca scenę logowania się klienta
     * @param primarystage Główne okno aplikacji
     * @throws Exception
     */
    @Override
    public void start(Stage primarystage) throws Exception {
        secondStageBuilded=false;
        stage=primarystage;
        stage.setTitle("Klient");
        GridPane layout = new GridPane();

        Button b1= new Button("Zaloguj się");
        Button b2 = new Button("wybierz katalog");
        TextField username = new TextField();
        username.setPromptText("Podaj login");
        TextField pathfield = new TextField();
        pathfield.setPromptText("Podaj ścieżkę");

        username.setMinWidth(173);
        pathfield.setMinWidth(173);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("src"));
        b2.setOnAction(e -> {
            File selectedDirectory = directoryChooser.showDialog(stage);
            pathfield.setText(selectedDirectory.getAbsolutePath());
        });
        b2.setMinWidth(100);
        GridPane.setConstraints(username,0,0,5,2);
        GridPane.setConstraints(pathfield,0,2,5,2);
        GridPane.setConstraints(b1,0,4,2,1);
        GridPane.setConstraints(b2,2,4,3,1);

        b1.setOnAction(e->logging(username,pathfield));
        layout.getChildren().addAll(username,pathfield,b1,b2);
        log = new Scene(layout,300,150);
        stage.setScene(log);
        stage.show();
    }

    /**
     *Metoda odpowiedzialna za połączenie klienta z serwerem oraz w wypadku nawiazania połączenia
     * odpowiedzialna za wywołanie metody utworzenia nowej sceny w przypadku gdy jest ona nie zainicjalizowana
     * @param username Pole zawierające nazwe użytkownika
     * @param path Pole zawierające ścieżkę do katalogu użytkownika
     * @return Metoda zwraca informacje o powodzeniu lub niepowodzeniu akcji połączenia z serwerem
     */
    private boolean logging(TextField username,TextField path){
        if(!Files.isDirectory(Paths.get(path.getText()))||username.getText().equals("")||path.getText().equals("")){
            return false;
        }
        try{
            InetAddress ip = InetAddress.getByName("localhost");
            s = new Socket(ip ,5400);
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
            /**
             *@param nawiązać połączenie;
             * */
            dos.writeUTF(username.getText());
            if (secondStageBuilded == false){
                createStage(stage);
                secondStageBuilded=true;
            }
            ClientReader t = new ClientReader(dis,dos,s,path.getText());
            username.clear();
            path.clear();
            t.setDaemon(true);
            t.start();
            stage.setScene(conn);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            return false;
        }
    }

    /**
     * Metoda odpowiedzialna za utworzenie nowej sceny ukazanej po udanym zalogowaniu się klienta
     * @param primarystage Główne okno aplikacji klienckiej
     * @throws Exception
     */
    private void createStage(Stage primarystage) throws Exception{
        String text=new String("Udało nawiązać się połączenie z serwerem");
        GridPane layout = new GridPane();
        label = new Label(text);
        Button button = new Button("Wyloguj się");
        button.setOnAction((e)->logOut());

        root=new TreeItem<>("root");
        root.setExpanded(true);
        tree=new TreeView<String>(root);
        tree.setShowRoot(false);

        Button b1= new Button("Wybierz plik");
        TextField user = new TextField();
        TextField fil = new TextField();
        b1.setOnAction(e->{
            FileDialog dialog = new FileDialog((Frame)null, "Wybierz plik");
            dialog.setMode(FileDialog.LOAD);
            dialog.setVisible(true);
            fil.setText(dialog.getDirectory()+dialog.getFile());
        });
        but = new Button("Wyślij");
        but.setMinWidth(100);
        b1.setMinWidth(100);
        button.setMinWidth(100);
        user.setPromptText("Podaj nazwe użytkownika");
        fil.setPromptText("Podaj swój plik");
        user.setMinWidth(100);
        fil.setMinWidth(100);

        GridPane.setConstraints(label,0,0,1,1);
        GridPane.setConstraints(tree,0,1,GridPane.REMAINING,1);
        GridPane.setConstraints(button,0,4,2,1);

        GridPane.setConstraints(user,1,2,1,1);
        GridPane.setConstraints(fil,0,2,1,1);
        GridPane.setConstraints(but,1,3,1,1);
        GridPane.setConstraints(b1,0,3,1,1);
        but.setOnAction((e)->send(user,fil));

        layout.getChildren().addAll(label,tree,button,but,user,fil,b1);
        conn=new Scene(layout,380,450);
    }

    /**
     *Metoda wylogowania się klienta i zamknięcia połączenia z serwerem
     */
    void logOut(){
        stage.setScene(log);
        try {
            s.close();
            dis.close();
            dos.close();
            root.getChildren().clear();
        } catch (IOException e) {
        }
    }

    /**
     * Metoda odpowiedzialna za próbę przesłanie pliku do innego klienta obecnego w archiwach serwerowych
     * @param user Nazwa klienta do którego ma być przesłany plik
     * @param path Ścieżka do pliku, który ma zostać przesłany
     */
    void send(TextField user, TextField path){
        try {
            dos.writeUTF("Client");
            dos.writeUTF(user.getText());
            dos.writeUTF(path.getText());
            user.clear();
            path.clear();
        }catch (IOException e){
            System.out.println("Nie udalo się przesłać wiadomości");
        }
    }

    /**
     *Wątek odpowiedzialny za komunikacje klienta z serwerem oraz wszystkie akcje
     * które zapewniają synchronizacje danych
     */
    class ClientReader extends Thread{
        Socket s;
        DataInputStream reader;
        DataOutputStream writter;
        boolean go=true;
        Path p;

        /**
         *Kontruktor przekazujący wątkowi wszystkie potrzebne obiekty konieczne do poprawnej komunikacji
         * @param is Strumień wejściowy gniazda klienta
         * @param os Strumień wyjściowy gniazda klienta
         * @param s Gniazdo klienta
         * @param p Ścieżka do folderu wskazanego przez użytkownika
         */
        public ClientReader(DataInputStream is,DataOutputStream os,Socket s,String p){
            this.s=s;
            reader=is;
            writter=os;
            this.p=Paths.get(p);
        }/**

        public void end(){
            go=false;
        }

        /**
         *Metoda odpowiedzialna za szereg akcji komunikacji klienta z serwerem
         */
        @Override
        public void run(){
            //inicjalizacja

            TreeItem<String> item = TreeFunctions.makeBranch(p.getFileName().toString(),root);

            TreeFunctions t = new TreeFunctions(p,item);
            t.setDaemon(true);
            t.start();

            while(go){
                try {
                    but.setDisable(true);
                    writter.writeUTF("");
                    FileSendingClient(p, s.getInputStream(), s.getOutputStream());
                    but.setDisable(false);
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    //e.printStackTrace();
                    break;
                }
            }
            //czyszczenie
            root.getChildren().clear();
            t.interrupt();
        }
    }
    /**
     *Metoda rekurencyjna odpowiedzialna za synchronizacje i przesyłanie plików między dwoma folderami znajdującymi
     * się na serwerze i na kliencie. Jest to metoda bliźniacza do funkcji FileSendingServer różniąca się jedynie
     * kolejnąścią wysyłania lub odbierania wiadomości lub plików
     * @param path Ścieżka do folderu który będzie synchronizowany z katalogiem na serwerze
     * @param is Strumień wejściowy gniazdka klienta
     * @param os Strumień wyjściowy gniazdka klienta
     * @throws Exception
     */
    void FileSendingClient(Path path,InputStream is,OutputStream os) throws Exception{
        DataOutputStream dos = new DataOutputStream(os);
        DataInputStream dis = new DataInputStream(is);
        String notification=new String();
        Iterator<Path> todivide = Files.list(path).iterator();
        while(todivide.hasNext()){
            Path div = todivide.next();
            notification = notification.concat(div.getFileName().toString()+'\n');
        }
        //Wysłanie swoich plików serwerowi
        dos.writeUTF(notification);
        //otrzymanie plików od serwera
        String[] receive = dis.readUTF().split("\n");
        String request=new String();
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
        //odebranie prośby o brakujące pliki
        String[] toSend = dis.readUTF().split("\n");
        //wysłanie prośby o brakujące pliki
        dos.writeUTF(request);
        //System.out.println(toSend);
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
        //odbiór plików
        receive=request.split("\n");
        //int FILE_SIZE=1024*1024*10;//10MB
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
        //zejsie nizej
        todivide = Files.list(path).iterator();
        while(todivide.hasNext()){
            Path div = todivide.next();
            if(Files.isDirectory(div)){
                FileSendingClient(div,is,os);
            }
        }
    }
}

