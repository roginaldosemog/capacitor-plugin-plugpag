package com.galago.plugins.plugpag;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Plugin Capacitor para integração com o terminal de pagamento PagBank via PlugPag SDK.
 *
 * <p>Toda comunicação com o SDK ocorre em thread de background ({@link ExecutorService})
 * para não bloquear a thread principal do Capacitor. Um {@link Semaphore} garante que
 * apenas uma operação financeira (pagamento ou estorno) aconteça por vez — tentativas
 * concorrentes aguardam até 10 s e, se o terminal ainda estiver ocupado, retornam erro.
 */
@CapacitorPlugin(name = "PlugPag")
public class PlugPagPlugin extends Plugin {
    private PlugPag implementation;
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();

    /** Impede doPayment e voidPayment simultâneos — o SDK PlugPag não suporta operações paralelas. */
    private final Semaphore operationMutex = new Semaphore(1);

    @Override
    public void load() {
        implementation = new PlugPag();
        implementation.initializeWrapper(getContext());
    }

    @PluginMethod
    public void abort(PluginCall call) {
        // Não usa operationMutex: precisa interromper o doPayment bloqueante que já detém o mutex.
        ioExecutor.submit(() -> {
            int result = implementation.abort();
            JSObject ret = new JSObject();
            ret.put("result", result);
            call.resolve(ret);
        });
    }

    @PluginMethod
    public void doPayment(PluginCall call) {
        executeBlockingOperation("paymentProgress", call);
    }

    @PluginMethod
    public void voidPayment(PluginCall call) {
        executeBlockingOperation("voidProgress", call);
    }

    /**
     * Executa doPayment ou voidPayment sob o mutex — impede operações bloqueantes paralelas.
     * O abort() deliberadamente NÃO usa o mutex: precisa interromper o doPayment que já o detém.
     */
    private void executeBlockingOperation(String eventName, PluginCall call) {
        ioExecutor.submit(() -> {
            boolean acquired = false;
            try {
                acquired = operationMutex.tryAcquire(10, TimeUnit.SECONDS);
                if (!acquired) {
                    call.reject("Terminal ocupado. Tente novamente ou cancele a operação.");
                    return;
                }

                JSObject result;
                if (eventName.equals("paymentProgress")) {
                    if (!call.hasOption("amount")) { call.reject("amount obrigatório"); return; }
                    result = implementation.doPayment(
                        call.getInt("type", 1),
                        call.getInt("amount"),
                        call.getInt("installmentType", 1),
                        call.getInt("installments", 1),
                        call.getString("userReference", ""),
                        call.getBoolean("printReceipt", true),
                        (msg, code) -> notifyProgress(eventName, msg, code)
                    );
                } else {
                    if (call.getString("transactionCode") == null) { call.reject("transactionCode obrigatório"); return; }
                    if (call.getString("transactionId") == null) { call.reject("transactionId obrigatório"); return; }
                    result = implementation.voidPayment(
                        call.getString("transactionCode"),
                        call.getString("transactionId"),
                        call.getBoolean("printReceipt", true),
                        (msg, code) -> notifyProgress(eventName, msg, code)
                    );
                }
                call.resolve(result);
            } catch (Exception e) {
                call.reject(e.getMessage());
            } finally {
                if (acquired) operationMutex.release();
            }
        });
    }

    /**
     * Executa uma operação bloqueante simples (sem eventos de progresso) sob o mutex.
     * Cobre: initialize (ativação) e reprintCustomerReceipt.
     */
    private void executeBlockingSimple(Callable<JSObject> action, PluginCall call) {
        ioExecutor.submit(() -> {
            boolean acquired = false;
            try {
                acquired = operationMutex.tryAcquire(10, TimeUnit.SECONDS);
                if (!acquired) {
                    call.reject("Terminal ocupado. Tente novamente ou cancele a operação.");
                    return;
                }
                JSObject result = action.call();
                if (result != null) call.resolve(result); else call.resolve();
            } catch (Exception e) {
                call.reject(e.getMessage());
            } finally {
                if (acquired) operationMutex.release();
            }
        });
    }

    private void notifyProgress(String eventName, String message, int code) {
        JSObject data = new JSObject();
        data.put("message", message);
        data.put("code", code);
        notifyListeners(eventName, data);
    }

    @PluginMethod
    public void isAuthenticated(PluginCall call) {
        ioExecutor.submit(() -> {
            JSObject ret = new JSObject();
            ret.put("value", implementation.isAuthenticated());
            call.resolve(ret);
        });
    }

    @PluginMethod
    public void isServiceBusy(PluginCall call) {
        ioExecutor.submit(() -> {
            JSObject ret = new JSObject();
            ret.put("value", implementation.isServiceBusy());
            call.resolve(ret);
        });
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        String code = call.getString("activationCode");
        if (code == null) { call.reject("Código obrigatório"); return; }
        executeBlockingSimple(() -> implementation.initialize(code), call);
    }

    @PluginMethod
    public void statusImpressora(PluginCall call) {
        ioExecutor.submit(() -> call.resolve(implementation.statusImpressora()));
    }

    @PluginMethod
    public void imprimirTexto(PluginCall call) {
        String mensagem = call.getString("mensagem", "");
        float size = call.getFloat("size", 20f);
        ioExecutor.submit(() -> {
            try {
                implementation.printText(mensagem, size);
                call.resolve();
            } catch (Exception e) {
                call.reject("Erro na impressão: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void printFromFile(PluginCall call) {
        String filePath = call.getString("filePath");
        if (filePath == null) { call.reject("filePath obrigatório"); return; }
        ioExecutor.submit(() -> {
            try {
                implementation.printFromFile(filePath);
                call.resolve();
            } catch (Exception e) {
                call.reject("Erro na impressão: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void reprintCustomerReceipt(PluginCall call) {
        executeBlockingSimple(() -> {
            implementation.reprintCustomerReceipt();
            return null;
        }, call);
    }

    @PluginMethod
    public void printPdfFromUrl(PluginCall call) {
        String url = call.getString("url");
        if (url == null) { call.reject("url obrigatório"); return; }
        ioExecutor.submit(() -> {
            try {
                implementation.printPdfFromUrl(url);
                call.resolve();
            } catch (Exception e) {
                call.reject("Erro ao imprimir PDF: " + e.getMessage());
            }
        });
    }
}