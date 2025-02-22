package gov.niptex.process;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class Query {
//    private static final String URL_EXAMPLE = "http://patft.uspto.gov/netacgi/nph-Parser?Sect1=PTO2&Sect2=HITOFF&u=%2Fnetahtml%2FPTO%2Fsearch-adv.htm&r=0&f=S&l=50&d=PTXT&RS=abc+AND+ui&Refine=Refine+Search&Query=abcd";
    private String firstPage;
    private Node[] listNode;
    private int size;

    public Query(String firstPage) {
        this.firstPage = firstPage;
    }

    private void getDataInPage(String page, int startIndex) throws IOException {
        Document document = Jsoup.connect(page).get();
        Elements elements = document.getElementsByTag("table").get(1).children().get(0).children(); //list tr
        elements.remove(0);
        int index = 0;
        for (Element e : elements) {
            String ID = e.getElementsByTag("td").get(1).getElementsByTag("a").get(0).text();
            ID = ID.replaceAll(",","");
            String name = e.getElementsByTag("td").get(3).getElementsByTag("a").get(0).text();
            listNode[startIndex + index] = new Node(ID, name);
            index++;
        }

    }

    protected Node[] query() throws IOException {
        Document document = Jsoup.connect(firstPage).timeout(50000).get();
        Element body = document.getElementsByTag("body").get(0);
        String textBody = body.text();
        if (textBody.isEmpty()){
            String string = document.getElementsByTag("head").get(0).getElementsByTag("meta").get(0).attr("content");
            string = string.replaceAll("&amp;","&");
            firstPage = string.replaceAll("1;URL=","http://patft.uspto.gov");
            document = Jsoup.connect(firstPage).timeout(5000).get();
            String ID = document.getElementsByTag("body").get(0).getElementsByTag("table").
                    get(2).getElementsByTag("tbody").get(0).
                    getElementsByTag("tr").get(0).getElementsByTag("td").get(1).text().replace(",","");
            String name = document.getElementsByTag("body").get(0).getElementsByTag("font").get(7).text();
            listNode= new Node[]{new Node(ID, name)};
            System.out.println(ID+"---"+name);
            this.size=1;
        }else {
            int size = getSizeOfResult(textBody);
            if (size==0) return null;
            int du = size % 50;
            int numPage = (du == 0) ? size / 50 : size / 50 + 1;
            listNode = new Node[size];
            if (numPage == 1) {
                getDataInPage(firstPage, 0);
            } else {
                ArrayList<String> listLoadPage = getAllPage(document, numPage, du);
                listLoadPage.add(0, this.firstPage);
                System.out.println("LOAD PAGE DONE!");
                System.out.println("-------------------");
                long startTime = System.nanoTime();
                int index = 0;
                for (String page : listLoadPage) {
                    getDataInPage(page, 50 * index);
                    index++;
                }// lay du lieu tu tat ca page
                System.out.println("GET DATA DONE!");
                System.out.println("--------------------");
                long endTime = System.nanoTime();
                System.out.println((endTime - startTime) / Math.pow(10, 9)); // in ra thoi gian lay du lieu
                System.out.println(size + " patents");
            }
            for (int i = 0; i < size; i++) {
                System.out.println(listNode[i].getID() + "---" + listNode[i].getName());
            } // in ra list node
        }
        return listNode;
    }

    private ArrayList<String> getAllPage(Element body, int numPage, int du) throws IOException {
        ArrayList<String> listPage = new ArrayList<>();

        Elements params = body.getElementsByTag("form").get(0).getElementsByAttributeValue("type", "HIDDEN");
        for (int i = 2; i < numPage; i++) {
            String url = "http://patft.uspto.gov/netacgi/nph-Parser?";
            for (Element e : params) {
                url = url + convertURL(e.attr("name")) + "=" + convertURL(e.attr("value")) + "&";
            }
            url = url + "NextList" + i + "=Next+50+Hits";
            listPage.add(url);
        }
        String lastURL = "http://patft.uspto.gov/netacgi/nph-Parser?";
        for (Element e : params) {
            lastURL = lastURL + convertURL(e.attr("name")) + "=" + convertURL(e.attr("value")) + "&";
        }
        if (du == 0) {
            lastURL = lastURL + "NextList" + numPage + "=Final+50+Hits";
        } else {
            lastURL = lastURL + "NextList" + numPage + "=Final+" + du + "+Hits";
        }
        listPage.add(lastURL);


        return listPage;
    }

    private String convertURL(String string) {

        String res = string.replace("/", "%2F");
        res = res.replace(" ", "+");
        return res;
    }

    private int getSizeOfResult(String textBody) {
        int size = 0;
        if (textBody.isEmpty()) return 1;
        int i0 = textBody.indexOf(":", textBody.indexOf(":") + 1) + 2;
        int i1 = textBody.indexOf("patents") - 1;
        size = Integer.parseInt(textBody.substring(i0, i1));
        this.size = size;
        return size;
    }

    public int getSize() {
        return size;
    }
}
