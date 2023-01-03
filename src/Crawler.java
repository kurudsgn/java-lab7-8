import java.net.*;

//создание нескольких потоков для обработки URL-адресов с корнем по указанному URL-адресу
public class Crawler {
    private URLPool pool;

    public int countThreads; //кол-во потоков
    //включение протокола в корневом URL-адресе (упрощение проверки посещенных URL-адресов)
    public Crawler(String root, int max) throws MalformedURLException {
        pool = new URLPool(max);
        URL rootURL = new URL(root);
        pool.add(new URLDepthPair(rootURL, 0));
    }

    //создание потоков CrawlerTask для обработки URL
    public void crawl() {
        for (int i = 0; i < countThreads; i++) {
            CrawlerTask crawler = new CrawlerTask(pool);
            Thread thread = new Thread(crawler);
            thread.start();
        }
        //проверяет пока все потоки не закончили работу
        while (pool.getWaitCount() != countThreads) {
            try {
                Thread.sleep(500);
            }
            //завершающий процесс, если количество всех потоков равно количеству потоков, обрабатываемых каждым методом

            catch (InterruptedException e) {
                System.out.println("Ignoring unexpected InterruptedException - " +
                        e.getMessage());
            }
        }
        pool.printURLs();
    }

    //запуск сканера, который сканирует каждый URL из базы
    public static void main(String[] args) {
        // предупредить пользователя о его синтаксической ошибке
        if (args.length != 3) {
            System.err.println("Usage: java Crawler <URL> <depth>");
            System.exit(1);
        }
        //создание примера класса Crawler и вызова метода обхода()
        try {
            Crawler crawler = new Crawler(args[0], Integer.parseInt(args[1]));
            crawler.countThreads = Integer.parseInt(args[2]);
            crawler.crawl();

        }
        catch (MalformedURLException e) { //обработка на неправ ввод
            System.err.println("Error: The URL " + args[0] + " is not valid");
            System.exit(1);
        }
        System.exit(0);
    }
}