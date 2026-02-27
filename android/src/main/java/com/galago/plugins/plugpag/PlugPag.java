package com.galago.plugins.plugpag;

import android.content.Context;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagActivationData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventListener;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInitializationResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagAbortResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagVoidData;
import com.getcapacitor.JSObject;

public class PlugPag {
    private br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag plugPagWrapper;

    // CORREÇÃO: Listener vazio para substituir o 'null' e evitar erro de non-null do Kotlin
    private final PlugPagEventListener emptyListener = new PlugPagEventListener() {
        @Override public void onEvent(PlugPagEventData data) { /* do nothing */ }
    };

    public interface PaymentEventListener {
        void onEvent(String message, int eventCode);
    }

    public void initializeWrapper(Context context) {
        if (plugPagWrapper == null) {
            // Uso de Contexto Global para evitar memory leaks em dispositivos Smart
            plugPagWrapper = new br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag(context.getApplicationContext());
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
        
        // CORREÇÃO CRÍTICA: Nunca passe null. Use o emptyListener.
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
            return ret;
        } else {
            String code = result.getErrorCode() != null ? result.getErrorCode() : "ERROR";
            throw new Exception(code + " - " + result.getMessage());
        }
    }
}