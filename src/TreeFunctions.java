import javafx.scene.control.TreeItem;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 *Wątek odpowiedzialny za synchronizację drzewa ukazanego w aplikacji
 * javaFX z realnym folderem znajdującym się na komputerze
 */
public class TreeFunctions extends Thread{
    Path path;
    TreeItem<String> root;

    /**
     *Konstruktor dla wątku
     * @param p Przekazana ścieżka do głównego katalogu
     * @param ti Przekazany element drzewa który jest wirtualnym odzwierciedleniem katalogu
     */
    TreeFunctions(Path p,TreeItem<String> ti){
        root=ti;
        path=p;
    }
    @Override
    /**
     *  Funkcja odpowiedzialna za synchronizacje katalogu realnego z odzwierciedleniem folderu na aplikacji JavaFX
     */
    public void run(){
        while(true){
            try {
                updateTree(path,root);
                TimeUnit.SECONDS.sleep(1);
                if(Thread.interrupted()){
                    break;
                }
            } catch (Exception e) {
                //e.printStackTrace();
                break;
            }

        }
    }

    /**
     *Metoda rekurencyjnie tworzy strukture drzewa, który ma ten sam wygląd, co folder przekazany jako parametr
     *
     * @param pth Ścieżka do folderu
     * @param rt Element na którym zostanie odzwierciedlony realny katalog
     */
    static void makeTree(Path pth, TreeItem<String> rt){
        try  {
            Iterator<Path> it = Files.list(pth).iterator();
            while(it.hasNext()){
                Path n = it.next();
                if (Files.isDirectory(n)) {
                    TreeItem<String> item = makeBranch(n.getFileName().toString(), rt);
                    makeTree(Paths.get(n.toString()), item);
                } else {
                    makeBranch(n.getFileName().toString(), rt);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Metoda tworząca nowy elemnt w drzewie
     * @param title Nazwa nowego elementu w drzewie
     * @param parent Element do którego zostanie dodany liść
     * @return Nowy element który został utworzony w trakcie wykonania metody
     */
    static TreeItem<String> makeBranch(String title, TreeItem<String> parent){
        TreeItem<String> item = new TreeItem<>(title);
        item.setExpanded(true);
        parent.getChildren().add(item);
        return item;
    }

    /**
     * Metoda odpowiedzialna za kontrole spójności pomiędzy folderem realnym a jego wirtualnym odzwierciedleniem
     * @param pth Ścieżka do folderu
     * @param root Wirtualne odzwierciedlenie folderu
     */
    static void updateTree(Path pth,TreeItem<String> root){
        try  {
            //usuwamy niepotrzebne
            for (int i=0;i<root.getChildren().size();++i){
                Iterator<Path> it =Files.list(pth).iterator();
                boolean missing =true;
                while (it.hasNext()){
                    Path p = it.next();
                    if(p.getFileName().toString().equals(root.getChildren().get(i).getValue())){
                        missing=false;
                        break;
                    }
                }
                if(missing)
                    root.getChildren().remove(i);
            }
            //dodajemy potrzebne
            Iterator<Path> it =Files.list(pth).iterator();
            while (it.hasNext()){
                Path p = it.next();
                boolean missing = true;
                for(int i =0;i<root.getChildren().size();++i){
                    if(root.getChildren().get(i).getValue().equals(p.getFileName().toString())){
                        missing=false;
                        break;
                    }
                }
                if(missing) {
                    TreeItem<String> item =makeBranch(p.getFileName().toString(),root);
                    if(Files.isDirectory(p))
                        makeTree(p,item);
                }
            }
            //zejscie nizej
            for(int i =0;i<root.getChildren().size();++i){
                if(Files.isDirectory(Paths.get(
                        pth.toString()
                                + File.separator
                                +root.getChildren().get(i).getValue()))
                ){
                    updateTree(
                            Paths.get(pth.toString()+File.separator+root.getChildren().get(i).getValue()),
                            root.getChildren().get(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
