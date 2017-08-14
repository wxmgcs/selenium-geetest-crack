package com.github.wycm;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeettestCrawler {
    private static String basePath = "src/main/resources/";
    private static String FULL_IMAGE_NAME = "full-image";
    private static String BG_IMAGE_NAME = "bg-image";
    private static int[][] moveArray = new int[52][2];
    private static boolean moveArrayInit = false;
    private static String INDEX_URL = "https://passport.feng.com/?r=user/register";
    private static WebDriver driver;

    static {
        System.setProperty("webdriver.chrome.driver", "/Users/wangyang/workspace/selenium/chromedriver_V2.30/chromedriver");
        if (System.getProperty("os.name").toLowerCase().contains("windows")){
            System.setProperty("webdriver.chrome.driver", "D:\\dev\\selenium\\chromedriver_V2.30\\chromedriver_win32\\chromedriver.exe");
        }
        driver = new ChromeDriver();
    }

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 10; i++){
            try {
                invoke();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        driver.quit();
    }
    private static void invoke() throws IOException, InterruptedException {
        //设置input参数
        driver.get(INDEX_URL);

        //通过[class=gt_slider_knob gt_show]
        By moveBtn = By.cssSelector(".gt_slider_knob.gt_show");
        waitForLoad(driver, moveBtn);
        WebElement moveElemet = driver.findElement(moveBtn);
        int i = 0;
        while (i++ < 15){
            int distance = getMoveDistance(driver);
            move(driver, moveElemet, distance - 6);
            By gtTypeBy = By.cssSelector(".gt_info_type");
            By gtInfoBy = By.cssSelector(".gt_info_content");
            waitForLoad(driver, gtTypeBy);
            waitForLoad(driver, gtInfoBy);
            String gtType = driver.findElement(gtTypeBy).getText();
            String gtInfo = driver.findElement(gtInfoBy).getText();
            System.out.println(gtType + "---" + gtInfo);
            /**
             * 再来一次：
             * 验证失败：
             */
            if(!gtType.equals("再来一次:") && !gtType.equals("验证失败:")){
                Thread.sleep(4000);
                System.out.println(driver);
                break;
            }
            Thread.sleep(4000);
        }
    }

    /**
     * 移动
     * @param driver
     * @param element
     * @param distance
     * @throws InterruptedException
     */
    public static void move(WebDriver driver, WebElement element, int distance) throws InterruptedException {
        int xDis = distance + 11;
        System.out.println("应平移距离：" + xDis);
        int moveX = new Random().nextInt(8) - 5;
        int moveY = 1;
        Actions actions = new Actions(driver);
        new Actions(driver).clickAndHold(element).perform();
        Thread.sleep(200);
        printLocation(element);
        actions.moveToElement(element, moveX, moveY).perform();
        System.out.println(moveX + "--" + moveY);
        printLocation(element);
        for (int i = 0; i < 22; i++){
            int s = 10;
            if (i % 2 == 0){
                s = -10;
            }
            actions.moveToElement(element, s, 1).perform();
//            printLocation(element);
            Thread.sleep(new Random().nextInt(100) + 150);
        }

        System.out.println(xDis + "--" + 1);
        actions.moveByOffset(xDis, 1).perform();
        printLocation(element);
        Thread.sleep(200);
        actions.release(element).perform();
    }
    private static void printLocation(WebElement element){
        Point point  = element.getLocation();
        System.out.println(point.toString());
    }
    /**
     * 等待元素加载，10s超时
     * @param driver
     * @param by
     */
    public static void waitForLoad(final WebDriver driver, final By by){
        new WebDriverWait(driver, 10).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                WebElement element = driver.findElement(by);
                if (element != null){
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * 计算需要平移的距离
     * @param driver
     * @return
     * @throws IOException
     */
    public static int getMoveDistance(WebDriver driver) throws IOException {
        String pageSource = driver.getPageSource();
        String fullImageUrl = getFullImageUrl(pageSource);
        FileUtils.copyURLToFile(new URL(fullImageUrl), new File(basePath + FULL_IMAGE_NAME + ".jpg"));
        String getBgImageUrl = getBgImageUrl(pageSource);
        FileUtils.copyURLToFile(new URL(getBgImageUrl), new File(basePath + BG_IMAGE_NAME + ".jpg"));
        initMoveArray(driver);
        restoreImage(FULL_IMAGE_NAME);
        restoreImage(BG_IMAGE_NAME);
        BufferedImage fullBI = ImageIO.read(new File(basePath + "result/" + FULL_IMAGE_NAME + "result3.jpg"));
        BufferedImage bgBI = ImageIO.read(new File(basePath + "result/" + BG_IMAGE_NAME + "result3.jpg"));
        for (int i = 0; i < bgBI.getWidth(); i++){
            for (int j = 0; j < bgBI.getHeight(); j++) {
                int[] fullRgb = new int[3];
                fullRgb[0] = (fullBI.getRGB(i, j)  & 0xff0000) >> 16;
                fullRgb[1] = (fullBI.getRGB(i, j)  & 0xff00) >> 8;
                fullRgb[2] = (fullBI.getRGB(i, j)  & 0xff);

                int[] bgRgb = new int[3];
                bgRgb[0] = (bgBI.getRGB(i, j)  & 0xff0000) >> 16;
                bgRgb[1] = (bgBI.getRGB(i, j)  & 0xff00) >> 8;
                bgRgb[2] = (bgBI.getRGB(i, j)  & 0xff);
                if(difference(fullRgb, bgRgb) > 255){
                    return i;
                }
            }
        }
        throw new RuntimeException("未找到需要平移的位置");
    }
    private static int difference(int[] a, int[] b){
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }
    /**
     * 获取move数组
     * @param driver
     */
    private static void initMoveArray(WebDriver driver){
        if (moveArrayInit){
            return;
        }
        Document document = Jsoup.parse(driver.getPageSource());
        Elements elements = document.select("[class=gt_cut_bg gt_show]").first().children();
        int i = 0;
        for(Element element : elements){
            Pattern pattern = Pattern.compile(".*background-position: (.*?)px (.*?)px.*");
            Matcher matcher = pattern.matcher(element.toString());
            if (matcher.find()){
                String width = matcher.group(1);
                String height = matcher.group(2);
                moveArray[i][0] = Integer.parseInt(width);
                moveArray[i++][1] = Integer.parseInt(height);
            } else {
                throw new RuntimeException("解析异常");
            }
        }
        moveArrayInit = true;
    }
    /**
     *还原图片
     * @param type
     */
    private static void restoreImage(String type) throws IOException {
        //把图片裁剪为2 * 26份
        for(int i = 0; i < 52; i++){
            cutPic(basePath + type +".jpg"
                    ,basePath + "result/" + type + i + ".jpg", -moveArray[i][0], -moveArray[i][1], 10, 58);
        }
        //拼接图片
        String[] b = new String[26];
        for(int i = 0; i < 26; i++){
            b[i] = String.format(basePath + "result/" + type + "%d.jpg", i);
        }
        mergeImage(b, 1, basePath + "result/" + type + "result1.jpg");
        //拼接图片
        String[] c = new String[26];
        for(int i = 0; i < 26; i++){
            c[i] = String.format(basePath + "result/" + type + "%d.jpg", i + 26);
        }
        mergeImage(c, 1, basePath + "result/" + type + "result2.jpg");
        mergeImage(new String[]{basePath + "result/" + type + "result1.jpg",
                basePath + "result/" + type + "result2.jpg"}, 2, basePath + "result/" + type + "result3.jpg");
        //删除产生的中间图片
        for(int i = 0; i < 52; i++){
            new File(basePath + "result/" + type + i + ".jpg").deleteOnExit();
        }
        new File(basePath + "result/" + type + "result1.jpg").deleteOnExit();
        new File(basePath + "result/" + type + "result2.jpg").deleteOnExit();
    }
    /**
     * 获取原始图url
     * @param pageSource
     * @return
     */
    private static String getFullImageUrl(String pageSource){
        String url = null;
        Document document = Jsoup.parse(pageSource);
        String style = document.select("[class=gt_cut_fullbg_slice]").first().attr("style");
        Pattern pattern = Pattern.compile("url\\(\"(.*)\"\\)");
        Matcher matcher = pattern.matcher(style);
        if (matcher.find()){
            url = matcher.group(1);
        }
        url = url.replace(".webp", ".jpg");
        System.out.println(url);
        return url;
    }
    /**
     * 获取带背景的url
     * @param pageSource
     * @return
     */
    private static String getBgImageUrl(String pageSource){
        String url = null;
        Document document = Jsoup.parse(pageSource);
        String style = document.select(".gt_cut_bg_slice").first().attr("style");
        Pattern pattern = Pattern.compile("url\\(\"(.*)\"\\)");
        Matcher matcher = pattern.matcher(style);
        if (matcher.find()){
            url = matcher.group(1);
        }
        url = url.replace(".webp", ".jpg");
        System.out.println(url);
        return url;
    }
    public static boolean cutPic(String srcFile, String outFile, int x, int y,
                                 int width, int height) {
        FileInputStream is = null;
        ImageInputStream iis = null;
        try {
            if (!new File(srcFile).exists()) {
                return false;
            }
            is = new FileInputStream(srcFile);
            String ext = srcFile.substring(srcFile.lastIndexOf(".") + 1);
            Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName(ext);
            ImageReader reader = it.next();
            iis = ImageIO.createImageInputStream(is);
            reader.setInput(iis, true);
            ImageReadParam param = reader.getDefaultReadParam();
            Rectangle rect = new Rectangle(x, y, width, height);
            param.setSourceRegion(rect);
            BufferedImage bi = reader.read(0, param);
            File tempOutFile = new File(outFile);
            if (!tempOutFile.exists()) {
                tempOutFile.mkdirs();
            }
            ImageIO.write(bi, ext, new File(outFile));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (iis != null) {
                    iis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }
    /**
     * 图片拼接 （注意：必须两张图片长宽一致哦）
     * @param files 要拼接的文件列表
     * @param type  1横向拼接，2 纵向拼接
     * @param targetFile 输出文件
     */
    private static void mergeImage(String[] files, int type, String targetFile) {
        int length = files.length;
        File[] src = new File[length];
        BufferedImage[] images = new BufferedImage[length];
        int[][] ImageArrays = new int[length][];
        for (int i = 0; i < length; i++) {
            try {
                src[i] = new File(files[i]);
                images[i] = ImageIO.read(src[i]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            int width = images[i].getWidth();
            int height = images[i].getHeight();
            ImageArrays[i] = new int[width * height];
            ImageArrays[i] = images[i].getRGB(0, 0, width, height, ImageArrays[i], 0, width);
        }
        int newHeight = 0;
        int newWidth = 0;
        for (int i = 0; i < images.length; i++) {
            // 横向
            if (type == 1) {
                newHeight = newHeight > images[i].getHeight() ? newHeight : images[i].getHeight();
                newWidth += images[i].getWidth();
            } else if (type == 2) {// 纵向
                newWidth = newWidth > images[i].getWidth() ? newWidth : images[i].getWidth();
                newHeight += images[i].getHeight();
            }
        }
        if (type == 1 && newWidth < 1) {
            return;
        }
        if (type == 2 && newHeight < 1) {
            return;
        }
        // 生成新图片
        try {
            BufferedImage ImageNew = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            int height_i = 0;
            int width_i = 0;
            for (int i = 0; i < images.length; i++) {
                if (type == 1) {
                    ImageNew.setRGB(width_i, 0, images[i].getWidth(), newHeight, ImageArrays[i], 0,
                            images[i].getWidth());
                    width_i += images[i].getWidth();
                } else if (type == 2) {
                    ImageNew.setRGB(0, height_i, newWidth, images[i].getHeight(), ImageArrays[i], 0, newWidth);
                    height_i += images[i].getHeight();
                }
            }
            //输出想要的图片
            ImageIO.write(ImageNew, targetFile.split("\\.")[1], new File(targetFile));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
