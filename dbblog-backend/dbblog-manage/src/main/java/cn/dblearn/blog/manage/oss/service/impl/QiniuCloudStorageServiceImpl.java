
package cn.dblearn.blog.manage.oss.service.impl;

import cn.dblearn.blog.common.exception.MyException;
import cn.dblearn.blog.common.exception.enums.ErrorEnum;
import cn.dblearn.blog.common.util.FilesUtils;
import cn.dblearn.blog.common.util.HttpUtil;
import cn.dblearn.blog.common.util.ThreadPoolManager;
import cn.dblearn.blog.config.CloudStorageConfig;
import cn.dblearn.blog.manage.oss.service.CloudStorageService;
import com.alibaba.druid.util.StringUtils;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.stereotype.Service;
import sun.net.www.http.HttpClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * QiniuCloudStorageService
 *
 * @author bobbi
 * @date 2018/10/22 12:35
 * @email 571002217@qq.com
 * @description
 */
@Service("cloudStorageService")
@Slf4j
public class QiniuCloudStorageServiceImpl extends CloudStorageService {
    private UploadManager uploadManager;
    private String token;
    private Auth auth;

    public QiniuCloudStorageServiceImpl(CloudStorageConfig config){
        this.config = config;
        //初始化
        init();
    }

    private void init(){
        uploadManager = new UploadManager(new Configuration(Zone.autoZone()));
        auth = Auth.create(config.getQiniuAccessKey(), config.getQiniuSecretKey());
        token = auth.uploadToken(config.getQiniuBucketName());
    }

    @Override
    public String upload(byte[] data, String path) {
        String url = null;
        try {
            token = auth.uploadToken(config.getQiniuBucketName());
            Response res = uploadManager.put(data, path, token);
            if (!res.isOK()) {
                throw new RuntimeException("上传七牛出错：" + res.toString());
            }
            url = config.getQiniuDomain() + "/" + path;
            try {
                HttpUtil.httpGet(url, null, null);
            }catch (Exception e) {
                log.error("同步失败");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MyException(ErrorEnum.OSS_CONFIG_ERROR);
        }
        return config.getQiniuDomain() + "/" + path;
    }

    @Override
    public String upload(InputStream inputStream, String path) {
        try {
            byte[] data = IOUtils.toByteArray(inputStream);
            return this.upload(data, path);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new MyException(ErrorEnum.OSS_CONFIG_ERROR);
        }
    }

    @Override
    public String uploadSuffix(byte[] data, String suffix) {
        return upload(data, getPath(config.getQiniuPrefix(), suffix));
    }

    @Override
    public String uploadSuffix(InputStream inputStream, String suffix) {
        return upload(inputStream, getPath(config.getQiniuPrefix(), suffix));
    }
}
