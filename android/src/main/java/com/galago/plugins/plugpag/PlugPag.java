package com.galago.plugins.plugpag;

import android.content.Context;
import android.util.Log;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagActivationData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventListener;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInitializationResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPrinterData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPrintResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPrinterListener;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagAbortResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagVoidData;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import com.getcapacitor.JSObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class PlugPag {
    private static final String TAG = "PlugPag";
    private br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag plugPagWrapper;
    private Context context;

    private final PlugPagEventListener emptyListener = new PlugPagEventListener() {
        @Override public void onEvent(PlugPagEventData data) { /* do nothing */ }
    };

    public interface PaymentEventListener {
        void onEvent(String message, int eventCode);
    }

    public void initializeWrapper(Context context) {
        this.context = context.getApplicationContext();
        if (plugPagWrapper == null) {
            plugPagWrapper = new br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag(this.context);
        }
    }

    public boolean isAuthenticated() { return plugPagWrapper.isAuthenticated(); }
    public boolean isServiceBusy() { return plugPagWrapper.isServiceBusy(); }

    public int abort() {
        PlugPagAbortResult abortResult = plugPagWrapper.abort();
        return abortResult.getResult(); 
    }

    public JSObject initialize(String activationCode) throws Exception {
        PlugPagActivationData data = new PlugPagActivationData(activationCode);
        PlugPagInitializationResult result = plugPagWrapper.initializeAndActivatePinpad(data);
        if (result.getResult() == br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag.RET_OK) {
            JSObject ret = new JSObject();
            ret.put("status", "success");
            return ret;
        } else {
            throw new Exception(result.getErrorCode() + ": " + result.getErrorMessage());
        }
    }

    public JSObject doPayment(int type, int amount, int installmentType, int installments, String userReference, boolean printReceipt, PaymentEventListener listener) throws Exception {
        if (!plugPagWrapper.isAuthenticated()) throw new Exception("POS não autenticado!");

        PlugPagPaymentData paymentData = new PlugPagPaymentData(
            type, amount, installmentType, installments, userReference, printReceipt, false, false
        );

        plugPagWrapper.setEventListener(createEventListener(listener));
        PlugPagTransactionResult result = plugPagWrapper.doPayment(paymentData);
        
        plugPagWrapper.setEventListener(emptyListener);
        return parseResult(result);
    }

    public JSObject voidPayment(String transactionCode, String transactionId, boolean printReceipt, PaymentEventListener listener) throws Exception {
        PlugPagVoidData voidData = new PlugPagVoidData(transactionCode, transactionId, printReceipt);
        
        plugPagWrapper.setEventListener(createEventListener(listener));
        PlugPagTransactionResult result = plugPagWrapper.voidPayment(voidData);
        
        plugPagWrapper.setEventListener(emptyListener);
        return parseResult(result);
    }

    private PlugPagEventListener createEventListener(PaymentEventListener listener) {
        return new PlugPagEventListener() {
            int passwordCount = 0;
            @Override
            public void onEvent(PlugPagEventData data) {
                String msg;
                if (data.getEventCode() == PlugPagEventData.EVENT_CODE_DIGIT_PASSWORD) {
                    passwordCount++;
                    msg = "SENHA: " + new String(new char[passwordCount]).replace("\0", "*");
                } else if (data.getEventCode() == PlugPagEventData.EVENT_CODE_NO_PASSWORD) {
                    passwordCount = 0;
                    msg = "DIGITE A SENHA";
                } else {
                    msg = data.getCustomMessage() != null ? data.getCustomMessage() : "PROCESSANDO...";
                }
                listener.onEvent(msg, data.getEventCode());
            }
        };
    }

    private JSObject parseResult(PlugPagTransactionResult result) throws Exception {
        if (result.getResult() == br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag.RET_OK) {
            JSObject ret = new JSObject();
            ret.put("transactionCode", result.getTransactionCode());
            ret.put("transactionId", result.getTransactionId());
            ret.put("message", result.getMessage());
            ret.put("errorCode", result.getErrorCode());
            ret.put("hostNsu", result.getHostNsu());
            ret.put("date", result.getDate());
            ret.put("time", result.getTime());
            ret.put("cardBrand", result.getCardBrand());
            ret.put("bin", result.getBin());
            ret.put("holder", result.getHolder());
            ret.put("userReference", result.getUserReference());
            ret.put("terminalSerialNumber", result.getTerminalSerialNumber());
            ret.put("amount", result.getAmount());
            ret.put("availableBalance", result.getAvailableBalance());
            ret.put("cardApplication", result.getCardApplication());
            ret.put("label", result.getLabel());
            ret.put("holderName", result.getHolderName());
            ret.put("extendedHolderName", result.getExtendedHolderName());
            ret.put("installments", result.getInstallments());
            return ret;
        } else {
            String code = result.getErrorCode() != null ? result.getErrorCode() : "ERROR";
            throw new Exception(code + " - " + result.getMessage());
        }
    }

    public JSObject printFromFile(String filePath) throws Exception {
        PlugPagPrinterData printerData = new PlugPagPrinterData(filePath, 4, 0);
        plugPagWrapper.setPrinterListener(new PlugPagPrinterListener() {
            @Override
            public void onError(PlugPagPrintResult result) {
                Log.e(TAG, "Erro na impressão: " + result.getMessage());
            }
            @Override
            public void onSuccess(PlugPagPrintResult result) {
                Log.d(TAG, "Impressão concluída");
            }
        });
        PlugPagPrintResult result = plugPagWrapper.printFromFile(printerData);
        JSObject ret = new JSObject();
        ret.put("result", result.getResult());
        return ret;
    }

    public void printText(String text) throws Exception {
        // O SDK do PlugPag espera um arquivo de imagem — o texto é renderizado em um Bitmap com Canvas.
        int paperWidth = 384; // 58 mm a 203 DPI (PagBank Smart / Pro)
        int padding = 8;
        float textSize = 26f;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setFakeBoldText(true);

        float lineHeight = paint.getFontSpacing();
        String[] lines = text.split("\n", -1);
        int bitmapHeight = (int) (lineHeight * lines.length) + padding * 2 + 80; // 80px extras para avanço de papel

        Bitmap bitmap = Bitmap.createBitmap(paperWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(bitmap);

        float y = padding - paint.ascent();
        for (String line : lines) {
            canvas.drawText(line, padding, y, paint);
            y += lineHeight;
        }

        File printDir = new File(context.getFilesDir(), "prints");
        printDir.mkdirs();
        File imageFile = new File(printDir, "print_" + System.currentTimeMillis() + ".jpg");
        imageFile.setReadable(true, false);

        FileOutputStream fos = new FileOutputStream(imageFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.flush();
        fos.close();
        bitmap.recycle();

        try {
            printFromFile(imageFile.getAbsolutePath());
        } finally {
            imageFile.delete();
        }
    }

    public JSObject statusImpressora() {
        JSObject ret = new JSObject();
        ret.put("status", plugPagWrapper.isAuthenticated() ? "IMPRESSORA OK" : "ERRO DESCONHECIDO");
        return ret;
    }

    public void reprintCustomerReceipt() {
        plugPagWrapper.reprintCustomerReceipt();
    }

    public void printPdfFromUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Alguns dispositivos Android mais antigos podem não ter o CA do servidor na trust store.
        // Um TrustManager permissivo é aplicado apenas nesta conexão de impressão.
        if (connection instanceof HttpsURLConnection) {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new java.security.SecureRandom());
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
            ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
        }

        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.connect();

        File printDir = new File(context.getFilesDir(), "prints");
        printDir.mkdirs();
        File pdfFile = new File(printDir, "doc_" + System.currentTimeMillis() + ".pdf");

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(pdfFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        // Renderiza cada página do PDF como bitmap e imprime
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer renderer = new PdfRenderer(pfd);
        try {
            int pageCount = renderer.getPageCount();
            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);

                int printWidth = 384;
                float scale = (float) printWidth / page.getWidth();
                int printHeight = (int) (page.getHeight() * scale);

                // ARGB_8888 é exigido pelo PdfRenderer
                Bitmap bitmap = Bitmap.createBitmap(printWidth, printHeight + 40, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                page.close();

                File pageFile = new File(printDir, "page_" + i + "_" + System.currentTimeMillis() + ".jpg");
                pageFile.setReadable(true, false);
                FileOutputStream fos = new FileOutputStream(pageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
                fos.close();
                bitmap.recycle();

                try {
                    printFromFile(pageFile.getAbsolutePath());
                } finally {
                    pageFile.delete();
                }
            }
        } finally {
            renderer.close();
            pfd.close();
            pdfFile.delete();
        }
    }
}