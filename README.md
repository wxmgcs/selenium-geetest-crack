## 摘要
分析验证码素材图片混淆原理，采用selenium模拟人拖动滑块过程，进而破解验证码。
## 人工验证的过程
1. 打开威锋网注册页面（https://passport.feng.com/?r=user/register）
2. 移动鼠标至小滑块，一张完整的图片会出现（如下图1）<br>
![](http://images2017.cnblogs.com/blog/1133483/201708/1133483-20170803201032006-1142077110.png)
3. 点击鼠标左键，图片中间会出现一个缺块（如下图2）<br>
![](http://images2017.cnblogs.com/blog/1133483/201708/1133483-20170803201057147-1194511710.png)
4. 移动小滑块正上方图案至缺块处
5. 验证通过

## selenium模拟验证的过程
1. 加载威锋网注册页面（https://passport.feng.com/?r=user/register）
2. 下载图片1和缺块图片2
3. 根据两张图片的差异计算平移的距离x
4. 模拟鼠标点击事件，点击小滑块向右移动x
5. 验证通过

## 详细分析
1. 打开chrome浏览器控制台，会发现图1所示的验证码图片并不是极验后台返回的原图。而是由多个div拼接而成（如下图3）<br>
![](http://images2017.cnblogs.com/blog/1133483/201708/1133483-20170814171904490-36719192.png)
通过图片显示div的style属性可知，极验后台把图片进行切割加错位处理。把素材图片切割成10 * 58大小的52张小图，再进行错位处理。在网页上显示的时候，再通过css的background-position属性对图片进行还原。以上的图1和图2都是经过了这种处理。在这种情况下，使用selenium模拟验证是需要对下载的验证码图片进行还原。如上图3的第一个div.gt_cut_fullbg_slice标签，它的大小为10px * 58px，其中style属性为```background-image: url("http://static.geetest.com/pictures/gt/969ffa43c/969ffa43c.webp"); background-position: -157px -58px;```会把该属性对应url的图片进行一个平移操作，以左上角为参考，向左平移157px，向上平移58px，图片超出部分不会显示。所以上图1所示图片是由26 * 2个10px * 58px大小的div组成（如下图4）。每一个小方块的大小58 * 10<br>
![](http://images2017.cnblogs.com/blog/1133483/201708/1133483-20170814174552537-911156554.png)
2. 下载图片并还原，上一步骤分析了图片具体的混淆逻辑，具体还原图片的代码实现如下，主要逻辑是把原图裁剪为52张小图，然后拼接成一张完整的图。<br>
    ```
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
    ```
    还原过程需要注意的是，后台返回错位的图片是312 * 116大小的。而网页上图片div的大小是260 * 116。
3. 计算平移距离，遍历图片的每一个像素点，当两张图的R、G、B之差的和大于255，说明该点的差异过大，很有可能就是需要平移到该位置的那个点，代码如下。
    ```
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
    ```
4. 模拟鼠标移动事件，这一步骤是最关键的步骤，极验验证码后台正是通过移动滑块的轨迹来判断是否为机器所为。整个移动轨迹的过程越随机越好，我这里提供一种成功率较高的移动算法，代码如下。
    ```
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
                printLocation(element);
                Thread.sleep(new Random().nextInt(100) + 150);
            }

            System.out.println(xDis + "--" + 1);
            actions.moveByOffset(xDis, 1).perform();
            printLocation(element);
            Thread.sleep(200);
            actions.release(element).perform();
        }
    ```