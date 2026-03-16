package com.galago.plugins.plugpag;

import android.content.Context;
import android.util.Log;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagActivationData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInstallment;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventListener;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInitializationResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPrinterData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPrintResult;
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
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import java.io.File;
import java.text.Normalizer;
import java.util.List;
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
        @Override public void onEvent(PlugPagEventData data) {}
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
        Log.d(TAG, "abort() chamado");
        PlugPagAbortResult abortResult = plugPagWrapper.abort();
        Log.d(TAG, "abort() resultado: " + abortResult.getResult());
        return abortResult.getResult();
    }

    public JSObject initialize(String activationCode) throws Exception {
        if (plugPagWrapper.isAuthenticated()) {
            Log.d(TAG, "initialize() ignorado — terminal já autenticado");
            JSObject ret = new JSObject();
            ret.put("status", "already_authenticated");
            return ret;
        }
        Log.d(TAG, "initialize() iniciando ativação");
        PlugPagActivationData data = new PlugPagActivationData(activationCode);
        PlugPagInitializationResult result = plugPagWrapper.initializeAndActivatePinpad(data);
        if (result.getResult() == br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag.RET_OK) {
            Log.d(TAG, "initialize() sucesso");
            JSObject ret = new JSObject();
            ret.put("status", "success");
            return ret;
        } else {
            Log.e(TAG, "initialize() falhou: " + result.getErrorCode() + " - " + result.getErrorMessage());
            throw new Exception(result.getErrorCode() + ": " + result.getErrorMessage());
        }
    }

    /**
     * Chama initializeAndActivatePinpad antes do doPayment para forçar atualização
     * de tabelas EMV (AID/CAPK) com feedback visual via paymentProgress.
     * Erros são suprimidos — a falha aqui não impede o pagamento.
     */
    public JSObject performPrePaymentInit(PaymentEventListener listener) {
        Log.d(TAG, "performPrePaymentInit() chamado");
        try {
            PlugPagActivationData data = new PlugPagActivationData("");
            plugPagWrapper.setEventListener(createEventListener(listener));
            PlugPagInitializationResult result = plugPagWrapper.initializeAndActivatePinpad(data);
            plugPagWrapper.setEventListener(emptyListener);
            JSObject ret = new JSObject();
            ret.put("status", result.getResult() == br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag.RET_OK ? "ok" : "failed");
            Log.d(TAG, "performPrePaymentInit() resultado: " + ret.getString("status"));
            return ret;
        } catch (Exception e) {
            plugPagWrapper.setEventListener(emptyListener);
            Log.w(TAG, "performPrePaymentInit() erro (ignorado): " + e.getMessage());
            JSObject ret = new JSObject();
            ret.put("status", "failed");
            return ret;
        }
    }

    public JSObject doPayment(int type, int amount, int installmentType, int installments, String userReference, boolean printReceipt, PaymentEventListener listener) throws Exception {
        if (!plugPagWrapper.isAuthenticated()) throw new Exception("POS não autenticado!");
        Log.d(TAG, "doPayment() type=" + type + " amount=" + amount + " installments=" + installments + " ref=" + userReference);

        PlugPagPaymentData paymentData = new PlugPagPaymentData(
            type, amount, installmentType, installments, userReference, printReceipt, true, false
        );

        plugPagWrapper.setEventListener(createEventListener(listener));
        PlugPagTransactionResult result = plugPagWrapper.doPayment(paymentData);
        plugPagWrapper.setEventListener(emptyListener);

        if (result.getResult() == br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag.RET_OK) {
            Log.d(TAG, "doPayment() aprovado — code=" + result.getTransactionCode() + " nsu=" + result.getHostNsu());
        } else {
            Log.e(TAG, "doPayment() negado — errorCode=" + result.getErrorCode() + " msg=" + result.getMessage());
        }
        return parseResult(result);
    }

    public JSObject voidPayment(String transactionCode, String transactionId, boolean printReceipt, PaymentEventListener listener) throws Exception {
        if (!plugPagWrapper.isAuthenticated()) throw new Exception("POS não autenticado!");
        Log.d(TAG, "voidPayment() transactionCode=" + transactionCode);

        PlugPagVoidData voidData = new PlugPagVoidData(transactionCode, transactionId, printReceipt);
        plugPagWrapper.setEventListener(createEventListener(listener));
        PlugPagTransactionResult result = plugPagWrapper.voidPayment(voidData);
        plugPagWrapper.setEventListener(emptyListener);

        if (result.getResult() == br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag.RET_OK) {
            Log.d(TAG, "voidPayment() estorno aprovado — code=" + result.getTransactionCode());
        } else {
            Log.e(TAG, "voidPayment() falhou — errorCode=" + result.getErrorCode() + " msg=" + result.getMessage());
        }
        return parseResult(result);
    }

    /**
     * Consulta as opções de parcelamento para um determinado valor e tipo.
     *
     * <p>O SDK consulta o serviço PagBank e retorna os valores reais calculados
     * com base no plano de recebimento do lojista.
     *
     * @param value           Valor da venda em centavos.
     * @param installmentType Modalidade: 2 = SELLER_INSTALLMENT, 3 = BUYER_INSTALLMENT.
     * @return Array JSON com as opções de parcelamento.
     */
    public JSArray calculateInstallments(int value, int installmentType) throws Exception {
        if (!plugPagWrapper.isAuthenticated()) throw new Exception("POS não autenticado!");
        Log.d(TAG, "calculateInstallments() value=" + value + " type=" + installmentType);

        List<PlugPagInstallment> list = plugPagWrapper.calculateInstallments(String.valueOf(value), installmentType);

        JSArray result = new JSArray();
        if (list != null) {
            for (PlugPagInstallment item : list) {
                JSObject obj = new JSObject();
                obj.put("installments",     item.getQuantity());
                obj.put("installmentValue", item.getAmount());
                obj.put("totalValue",       item.getTotal());
                result.put(obj);
            }
        }
        Log.d(TAG, "calculateInstallments() retornou " + result.length() + " opções");
        return result;
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

    public void printFromFile(String filePath) throws Exception {
        PlugPagPrinterData printerData = new PlugPagPrinterData(filePath, 4, 0);
        PlugPagPrintResult result = plugPagWrapper.printFromFile(printerData);
        if (result.getResult() != 0) {
            throw new Exception("Falha na impressão: " + result.getMessage());
        }
    }

    public void printText(String text) throws Exception {
        printText(text, 20f);
    }

    /**
     * Normaliza o texto para ASCII removendo marcas diacríticas (acentos, cedilha, etc.).
     * Necessário porque a impressora térmica / Typeface.MONOSPACE não suporta Latin Extended
     * de forma confiável — caracteres multi-byte UTF-8 aparecem como sequências Latin-1 corrompidas.
     * Ex: "Brasília" → "Brasilia", "São Paulo" → "Sao Paulo".
     */
    private static String normalizeForPrinter(String text) {
        if (text == null) return "";
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "");
    }

    public void printText(String text, float textSize) throws Exception {
        // O SDK PlugPag aceita apenas arquivos de imagem para impressão.
        // O texto é renderizado em Bitmap monoespaçado (384 px = 58 mm a 203 DPI) e salvo como JPEG temporário.
        // Referência de chars/linha por tamanho de fonte: size=18 → ~34 | size=20 → ~28 | size=26 → ~23
        text = normalizeForPrinter(text);
        int paperWidth = 384;
        int padding = 8;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.MONOSPACE);

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

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } finally {
            bitmap.recycle();
        }
        imageFile.setReadable(true, false);

        try {
            printFromFile(imageFile.getAbsolutePath());
        } finally {
            imageFile.delete();
        }
    }

    /**
     * Verifica a conexão com o serviço PlugPag via IPC.
     *
     * <p><b>Atenção:</b> usa {@code isAuthenticated()} como proxy — não detecta falhas físicas
     * da impressora (papel acabado, cabeçote, etc.). Erros físicos só aparecem ao tentar imprimir.
     *
     * @return {@code "IMPRESSORA OK"} se autenticado, {@code "TERMINAL NAO AUTENTICADO"} caso contrário.
     */
    public JSObject statusImpressora() {
        JSObject ret = new JSObject();
        ret.put("status", plugPagWrapper.isAuthenticated() ? "IMPRESSORA OK" : "TERMINAL NAO AUTENTICADO");
        return ret;
    }

    public void reprintCustomerReceipt() throws Exception {
        PlugPagPrintResult result = plugPagWrapper.reprintCustomerReceipt();
        if (result != null && result.getResult() != 0) {
            throw new Exception("Falha na reimpressão: " + result.getMessage());
        }
    }

    public void printPdfFromUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Dispositivos Android mais antigos podem não ter o CA do servidor na trust store.
        // TrustManager permissivo aplicado somente aqui — não afeta outras conexões do app.
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

        int httpStatus = connection.getResponseCode();
        if (httpStatus < 200 || httpStatus >= 300) {
            connection.disconnect();
            throw new Exception("Falha ao baixar PDF: HTTP " + httpStatus);
        }

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
        } finally {
            connection.disconnect();
        }

        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
        try {
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
                    try (FileOutputStream fos = new FileOutputStream(pageFile)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.flush();
                    }
                    bitmap.recycle();
                    pageFile.setReadable(true, false);

                    try {
                        printFromFile(pageFile.getAbsolutePath());
                    } finally {
                        pageFile.delete();
                    }
                }
            } finally {
                renderer.close();
            }
        } finally {
            pfd.close();
            pdfFile.delete();
        }
    }
}