package com.leansoft.bigqueue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tony on 2017/7/26.
 */
public class JavaFile
{
    public List filereadline(String url) throws IOException
    {

        //BufferedReader是可以按行读取文件
        FileInputStream inputStream = new FileInputStream(url);//"D://logs//sql.log"
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        List list = new ArrayList();
        String str = null;
        while((str = bufferedReader.readLine()) != null)
        {
            if(str.isEmpty()){
                continue;
            }
            list.add(str);
           // System.out.println("sssss--->"+str);

        }
        //close
        inputStream.close();
        bufferedReader.close();
        return list;
    }

    public static void main(String[] args) {
/*        JavaFile javaFile = new JavaFile();
        try {
            List lists = javaFile.filereadline("D://logs//sql.log");
                  System.out.println("ggggg--->"+lists);
            for (int i = 0; i < lists.size(); i++) {
                 System.out.println("aaaaa--->"+lists.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }
}