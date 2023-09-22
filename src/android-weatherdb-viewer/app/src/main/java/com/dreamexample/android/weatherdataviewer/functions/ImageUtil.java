package com.dreamexample.android.weatherdataviewer.functions;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.dreamexample.android.weatherdataviewer.WeatherApplication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

/**
 * データ可視化フラグメント用共通メソッド
 */
public class ImageUtil {
//    private static final String TAG = AppImageFragUtil.class.getSimpleName();

    /**
     * リクエストヘッダー("X-Request-Image-Size")に端末サイズ情報文字列を設定する
     *  (例) X-Request-Image-Size: 1064x1593x1.50
     * @param headers リクエストヘッダー用のマップオブジェクト
     * @param imageWd ImageViewの幅
     * @param imageHt ImageViewの高さ
     * @param density デバイス密度
     */
    public static void appendImageSizeToHeaders(Map<String, String> headers,
                                               int imageWd, int imageHt, float density) {
        // サイズは半角数値なのでロケールは "US"とする
        String imgSize = String.format(Locale.US, "%dx%dx%f", imageWd, imageHt, density);
        if (headers.containsKey(WeatherApplication.REQUEST_IMAGE_SIZE_KEY)) {
            // キーが存在すれば上書き
            headers.replace(WeatherApplication.REQUEST_IMAGE_SIZE_KEY, imgSize);
        } else {
            // なければ追加
            headers.put(WeatherApplication.REQUEST_IMAGE_SIZE_KEY, imgSize);
        }
    }

    /**
     * ImageViewのビットマップをファイル保存
     * @param context Activity
     * @param bitmap ImageViewのビットマップ
     * @param saveName 保存名
     * @return 保存完了なら保存ファイル(絶対パス)
     */
    public static String saveBitmapToPng(Context context,
                                          Bitmap bitmap, String saveName) throws IOException {
        // http://www.java2s.com/example/android/graphics/save-bitmap-to-a-file-path.html
        //  save Bitmap to a File Path - Android Graphics
        if (bitmap == null) {
            return null;
        }

        File rootPath = context.getFilesDir();
        if (rootPath.exists()) {
            Path savePath = Paths.get(rootPath.getAbsolutePath(), saveName);
            File file = savePath.toFile();
            if (file.exists()) {
                file.delete();
            }
            try(BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    public static Bitmap readBitmapFromAbsolutePath(String absolutePath) throws IOException {
        File file = new File(absolutePath);
        if (file.exists()) {
            Bitmap result;
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
                result = BitmapFactory.decodeStream(in);
            }
            return result;
        } else {
            return null;
        }
    }

}
