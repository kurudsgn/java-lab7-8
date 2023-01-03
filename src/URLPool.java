import java.util.*;

//хранение списка поисковых URL и глубины поиска
public class URLPool {
    private int maxDepth; //максимальная глубина до которой ведется поиск
    private int waitCount = 0; //количество текущих потоков в режиме ожидания
    private LinkedList<URLDepthPair> rawURLs; //необработанные URL-адреса
    private LinkedList<URLDepthPair> processedURLs; //обработанные URL-адреса
    private HashSet<String> scanURLs; //пройденные URL

    //URLPool с максимальной указанной глубиной
    public URLPool(int max) {
        rawURLs = new LinkedList<URLDepthPair>();
        processedURLs = new LinkedList<URLDepthPair>();
        scanURLs = new HashSet<String>();
        maxDepth = max;
    }

    //возвращает количество ожидающих потоков
    public synchronized int getWaitCount() {
        return waitCount;
    }

    //добавление пары URLDepthPair в
    public synchronized void add(URLDepthPair nextPair) {
        String newURL = nextPair.getURL().toString();
        String modifyURL = (newURL.endsWith("/")) ? newURL.substring(0, newURL.length() -1) : newURL;// убираем / в конце ссылки
        if (scanURLs.contains(modifyURL))
            return;
        scanURLs.add(modifyURL);

        if (nextPair.getDepth() < maxDepth) {
            rawURLs.add(nextPair);
            notify(); //уведомлять поток ожидающий, когда новый URL добавляется в пул если может продолжать поиск < maxDepth
        }
        processedURLs.add(nextPair);
    }

    // получает URL из списка не проверенных
    public synchronized URLDepthPair get() {
        //ожидание, если в настоящее время URL недоступен
        while (rawURLs.size() == 0) {
            waitCount++; //увеличивается непосредственно перед вызовом wait()
            try {
                wait();
            }
            catch (InterruptedException e) {
                System.out.println("Ignoring unexpected InterruptedException - " +
                        e.getMessage());
            }
            waitCount--; //уменьшается сразу после выхода из режима ожидания
        }
        return rawURLs.removeFirst();
    }

    //отображение всех обработанных URL
    public synchronized void printURLs() {
        System.out.println("\nUnique URLs Found: " + processedURLs.size());
        while (!processedURLs.isEmpty()) {
            System.out.println(processedURLs.removeFirst());
        }
    }
}