import java.io.*;
import java.net.*;
import java.util.regex.*;

//веб-сканирование в несколько потоков (проверка веб-страниц, добавление новых ссылок в пул для сканирования в несколько потоков)
public class CrawlerTask implements Runnable { // две реализации нескольких потоков (реализует, реализация интерфейса Runnable) и примеры класса Thread
    public static final String LINK_REGEX = "href\\s*=\\s*\"([^$^\"]*)\"";
    public static final Pattern LINK_PATTERN = Pattern.compile(LINK_REGEX, Pattern.CASE_INSENSITIVE);
    private URLPool pool;

    public CrawlerTask(URLPool p) {
        pool = p;
    }

    //сокет для отправки HTTP-запроса на веб-страницу URLDepthPair
    public Socket sendRequest(URLDepthPair nextPair)
            throws UnknownHostException, SocketException, IOException { // сказать, что блок может дать исключение
        //создание сокета
        Socket socket = new Socket(nextPair.getHost(), 80);
        socket.setSoTimeout(1500);

        //запрос ресурсов от хоста веб-страницы
        OutputStream out = socket.getOutputStream(); // создаем сокет
        PrintWriter writer = new PrintWriter(out, true);
        writer.println("GET " + nextPair.getDocPath() + " HTTP/1.1");//запрос
        writer.println("Host: " + nextPair.getHost());
        writer.println("Connection: close");
        writer.println();

        return socket;
    }

    //метод обработки URL-адресов путем поиска всех ссылок и добавления их в URLPool.
    public void processURL(URLDepthPair url) throws IOException {
        Socket socket;
        try { // блок кода, где может быть исключение
            socket = sendRequest(url);
        }
        // исключения
        catch (UnknownHostException e) { // блок, в котором обрабатывается исключение
            System.err.println("Host "+ url.getHost() + " couldn't be determined");
            return;
        }
        catch (SocketException e) {
            System.err.println("Error with socket connection: " + url.getURL() +
                    " - " + e.getMessage());
            return;
        }
        catch (IOException e) {
            System.err.println("Couldn't retrieve page at " + url.getURL() +
                    " - " + e.getMessage());
            return;
        }

        InputStream input = socket.getInputStream();//получаем ответ на запрос
        BufferedReader reader = new BufferedReader(new InputStreamReader(input)); // переводим наш ответ на запрос для обратки

        String line;
        //поиск ссылок на странице
        while ((line = reader.readLine()) != null) { //получение пары URL-Depth из пула
            Matcher LinkFinder = LINK_PATTERN.matcher(line);//получение веб-страницы по URL-адресу
            while (LinkFinder.find()) { //искать другие URL-адреса на странице
                String newURL = LinkFinder.group(1);
                URL newSite;
                try {
                    if (URLDepthPair.isAbsolute(newURL)) // если ссылка абсолют то мы запоминаем
                        newSite = new URL(newURL);
                    else newSite = new URL(url.getURL(), newURL); // если нет приводим её к абсолюту
                    //добавление в пул новой пары URL-Depth (глубина на 1 больше предыдущей)
                    pool.add(new URLDepthPair(newSite, url.getDepth() + 1));
                }
                catch (MalformedURLException e) {
                    System.err.println("Error with URL - " + e.getMessage());
                }
            }
        }
        reader.close();
        //закрытие сокета
        try {
            socket.close();
        }
        catch (IOException e) {
            System.err.println("Couldn't close connection to " + url.getHost() +
                    " - " + e.getMessage());
        }
    }

    //метод для первого URL в пуле
    @Override
    public void run() { // этот метод передает любому конструктору класса Thread
        URLDepthPair nextPair;
        while (true) {
            nextPair = pool.get();
            try {
                processURL(nextPair);
            }
            catch (IOException e) {
                System.err.println("Error reading the page at " + nextPair.getURL() +
                        " - " + e.getMessage());
            }
        }
    }
}
