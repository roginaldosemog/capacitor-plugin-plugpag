package com.galago.plugins.plugpag;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CapacitorPlugin(name = "PlugPag")
public class PlugPagPlugin extends Plugin {

    private PlugPag implementation;

    /**
     * Pool de threads dedicado para operações bloqueantes do PlugPag SDK.
     * Equivalente ao Dispatchers.IO do Kotlin: permite que doPayment e abort
     * rodem CONCORRENTEMENTE em threads separadas do mesmo pool, sem bloquear
     * o HandlerThread do Capacitor (getBridge().execute()).
     */
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();

    @Override
    public void load() {
        implementation = new PlugPag();
        if (getContext() != null) {
            implementation.initializeWrapper(getContext());
            com.getcapacitor.Logger.info("PlugPag", "SDK Wrapper inicializado com sucesso.");
        }
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
    public void abort(PluginCall call) {
        ioExecutor.submit(() -> {
            int result = implementation.abort();
            JSObject ret = new JSObject();
            ret.put("result", result);
            call.resolve(ret);
        });
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        String activationCode = call.getString("activationCode");

        if (activationCode == null) {
            call.reject("Código de ativação é obrigatório.");
            return;
        }

        ioExecutor.submit(() -> {
            try {
                JSObject result = implementation.initialize(activationCode);
                call.resolve(result);
            } catch (Exception e) {
                call.reject(e.getMessage());
            }
        });
    }

    @PluginMethod
    public void doPayment(PluginCall call) {
        Integer type = call.getInt("type", 1);
        Integer amount = call.getInt("amount");
        Integer installmentType = call.getInt("installmentType", 1);
        Integer installments = call.getInt("installments", 1);
        String userReference = call.getString("userReference", "");
        Boolean printReceipt = call.getBoolean("printReceipt", true);

        if (amount == null) {
            call.reject("O valor (amount) é obrigatório.");
            return;
        }

        ioExecutor.submit(() -> {
            try {
                JSObject result = implementation.doPayment(
                    type, amount, installmentType, installments, userReference, printReceipt,

                    (message, eventCode) -> {
                        JSObject eventData = new JSObject();
                        eventData.put("message", message);
                        eventData.put("code", eventCode);
                        notifyListeners("paymentProgress", eventData);
                    }
                );

                call.resolve(result);
            } catch (Exception e) {
                call.reject(e.getMessage());
            }
        });
    }

    @PluginMethod
    public void voidPayment(PluginCall call) {
        String transactionCode = call.getString("transactionCode");
        String transactionId = call.getString("transactionId");
        Boolean printReceipt = call.getBoolean("printReceipt", true);

        if (transactionCode == null) {
            call.reject("O transactionCode é obrigatório.");
            return;
        }

        if (transactionId == null) {
            call.reject("O transactionId é obrigatório.");
            return;
        }

        ioExecutor.submit(() -> {
            try {
                JSObject result = implementation.voidPayment(
                    transactionCode, transactionId, printReceipt,
                    (message, eventCode) -> {
                        JSObject eventData = new JSObject();
                        eventData.put("message", message);
                        eventData.put("code", eventCode);
                        notifyListeners("voidProgress", eventData);
                    }
                );

                call.resolve(result);
            } catch (Exception e) {
                call.reject(e.getMessage());
            }
        });
    }
}
