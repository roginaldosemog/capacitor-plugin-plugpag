package com.galago.plugins.plugpag;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@CapacitorPlugin(name = "PlugPag")
public class PlugPagPlugin extends Plugin {
    private PlugPag implementation;
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    private final Semaphore operationMutex = new Semaphore(1);

    @Override
    public void load() {
        implementation = new PlugPag();
        implementation.initializeWrapper(getContext());
    }

    @PluginMethod
    public void abort(PluginCall call) {
        ioExecutor.submit(() -> {
            int result = implementation.abort();
            JSObject ret = new JSObject();
            ret.put("result", result);
            call.resolve(ret);
        });
    }

    @PluginMethod
    public void doPayment(PluginCall call) {
        executeSharedOperation("paymentProgress", call);
    }

    @PluginMethod
    public void voidPayment(PluginCall call) {
        executeSharedOperation("voidProgress", call);
    }

    private void executeSharedOperation(String eventName, PluginCall call) {
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

    private void notifyProgress(String eventName, String message, int code) {
        JSObject data = new JSObject();
        data.put("message", message);
        data.put("code", code);
        notifyListeners(eventName, data);
    }

    @PluginMethod
    public void isAuthenticated(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("value", implementation.isAuthenticated());
        call.resolve(ret);
    }

    @PluginMethod
    public void isServiceBusy(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("value", implementation.isServiceBusy());
        call.resolve(ret);
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        String code = call.getString("activationCode");
        if (code == null) { call.reject("Código obrigatório"); return; }
        ioExecutor.submit(() -> {
            try {
                call.resolve(implementation.initialize(code));
            } catch (Exception e) { call.reject(e.getMessage()); }
        });
    }

    @PluginMethod
    public void statusImpressora(PluginCall call) {
        call.resolve(implementation.statusImpressora());
    }

    @PluginMethod
    public void imprimirTexto(PluginCall call) {
        String mensagem = call.getString("mensagem", "");
        ioExecutor.submit(() -> {
            try {
                implementation.printText(mensagem);
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
                call.resolve(implementation.printFromFile(filePath));
            } catch (Exception e) {
                call.reject("Erro na impressão: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void reprintCustomerReceipt(PluginCall call) {
        ioExecutor.submit(() -> {
            try {
                implementation.reprintCustomerReceipt();
                call.resolve();
            } catch (Exception e) {
                call.reject("Erro ao reimprimir: " + e.getMessage());
            }
        });
    }
}