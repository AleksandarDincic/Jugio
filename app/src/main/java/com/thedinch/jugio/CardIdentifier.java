package com.thedinch.jugio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;

public class CardIdentifier {

    public static final int IMAGE_WIDTH = 200;
    public static final int IMAGE_HEIGHT = 200;


    public static final String MODEL_PATH = "ident_model_v35.tflite";

    public static final String CARD_DATA_JSON_PATH = "data_trunc.json";
    public static final String CARD_EMBEDDINGS_JSON_PATH = "embeddings_v35.json";

    public static JSONArray cardDataJSON = null;
    public static HashMap<String, String> cardNames = new HashMap<>();
    public static JSONObject cardEmbeddingsJSON = null;

    private static float[][][] preprocessInputCaffe(Bitmap bitmap) {
        final float[] imagenet_means_caffe = new float[]{103.939f, 116.779f, 123.68f};

        float[][][] result = new float[bitmap.getHeight()][bitmap.getWidth()][3];   // assuming rgb
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {

                final int px = bitmap.getPixel(x, y);

                result[y][x][0] = (Color.blue(px) - imagenet_means_caffe[0]);
                result[y][x][1] = (Color.green(px) - imagenet_means_caffe[1]);
                result[y][x][2] = (Color.red(px) - imagenet_means_caffe[2]);
            }
        }

        return result;
    }

    private static String loadJSONFromAsset(String filename, Context context) {
        String json = null;
        try {
            StringBuilder textBuilder = new StringBuilder();
            InputStream is = context.getAssets().open(filename);
            try (Reader reader = new BufferedReader(new InputStreamReader
                    (is, Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }
            json = textBuilder.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static void initJSON(Context context) {
        try {
            System.out.println("JSON INIT START");
            if (cardDataJSON == null) {
                cardDataJSON = new JSONArray(loadJSONFromAsset(CARD_DATA_JSON_PATH, context));

                for (int i = 0; i < cardDataJSON.length(); ++i) {
                    JSONObject jsonObj = cardDataJSON.getJSONObject(i);
                    String id = jsonObj.getString("id");
                    String name = jsonObj.getString("name");
                    cardNames.put(id, name);
                }
            }
            if (cardEmbeddingsJSON == null)
                cardEmbeddingsJSON = new JSONObject(loadJSONFromAsset(CARD_EMBEDDINGS_JSON_PATH, context));
            System.out.println("JSON INIT END");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    Context context;
    Interpreter interpreter;

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public CardIdentifier(Context context) {
        this.context = context;
    }

    private Bitmap toGrayscale(Bitmap bmpOriginal) {
        Bitmap bmpGrayscale = Bitmap.createBitmap(bmpOriginal);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public static double similarity(float[] vectorA, float[] vectorB) {
        /*double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));*/
        double norm = 0.0;
        for (int i = 0; i < vectorA.length; ++i) {
            norm += Math.pow(vectorA[i] - vectorB[i], 2);
        }
        return Math.sqrt(norm);
    }

    private String findNearestCard(float[] embedding) {
        try {
            float[] embeddingBuffer = new float[256];

            String currentBestCard = null;
            double currentBestScore = -1;

            Iterator<String> keys = cardEmbeddingsJSON.keys();
            while (keys.hasNext()) {
                String cardClass = keys.next();
                JSONArray cardClassEmbeddings = cardEmbeddingsJSON.getJSONArray(cardClass);
                for (int i = 0; i < cardClassEmbeddings.length(); ++i) {
                    JSONArray cardClassEmbedding = cardClassEmbeddings.getJSONArray(i);
                    for (int j = 0; j < 256; ++j) {
                        embeddingBuffer[j] = (float) cardClassEmbedding.getDouble(j);
                    }

                    double thisScore = similarity(embedding, embeddingBuffer);
                    if (currentBestCard == null || thisScore < currentBestScore) {
                        currentBestCard = cardClass;
                        currentBestScore = thisScore;
                    }
                }
            }

            return currentBestCard;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void identify(DetectionResult result) {
        Bitmap preprocessedBitmap = Bitmap.createScaledBitmap(result.bitmap, IMAGE_WIDTH, IMAGE_HEIGHT, true);
        preprocessedBitmap = toGrayscale(preprocessedBitmap);
        //result.bitmap = preprocessedBitmap;

        float[][][][] imgValues = new float[1][IMAGE_WIDTH][IMAGE_HEIGHT][3];

        imgValues[0] = preprocessInputCaffe(preprocessedBitmap);

        float[][] output = new float[1][256];

        interpreter.run(imgValues, output);

        String nearestCardId = findNearestCard(output[0]);

        if(nearestCardId != null) {
            String nearestCardName = cardNames.get(nearestCardId);
            result.cardName = nearestCardName;
        } else{
            result.cardName = "Error";
        }
    }

    public void initCardIdentifier() {
        try {
            while (cardDataJSON == null || cardEmbeddingsJSON == null) {
            }
            System.out.println("INIT START");
            interpreter = new Interpreter(loadModelFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
