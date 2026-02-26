package com.galago.plugins.plugpag;

import android.content.Context;

import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagActivationData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventListener;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagInitializationResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult;
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagVoidData;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;

public class PlugPag {
    private br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag plugPagWrapper;

    public interface PaymentEventListener {
        void onEvent(String message, int eventCode);
    }

    public boolean isAuthenticated() {
        return plugPagWrapper.isAuthenticated();
    }

    public boolean isServiceBusy() {
        return plugPagWrapper.isServiceBusy();
    }

    public void initializeWrapper(Context context) {
        if (plugPagWrapper == null) {
            plugPagWrapper = new br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag(context);
        }
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
        // 1. Verificação de ocupado antes de iniciar
        if (plugPagWrapper.isServiceBusy()) {
            throw new Exception("O serviço já está ocupado com outra operação.");
        }

        if (!plugPagWrapper.isAuthenticated()) {
            throw new Exception("POS não autenticado!");
        }

        try {
            // Monta os dados do pagamento
            PlugPagPaymentData paymentData = new PlugPagPaymentData(
                type, amount, installmentType, installments, userReference, printReceipt, false, false
            );

            // Escuta os eventos da maquininha (senha, processando, etc)
            plugPagWrapper.setEventListener(new PlugPagEventListener() {
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
                        msg = data.getCustomMessage() != null ? data.getCustomMessage() : "AGUARDANDO...";
                    }
                    
                    // Dispara o evento para o bridge
                    listener.onEvent(msg, data.getEventCode());
                }
            });

            // Executa a transação (isso bloqueia a thread, por isso rodaremos em background no Plugin)
            PlugPagTransactionResult result = plugPagWrapper.doPayment(paymentData);

            if (result.getResult() == br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag.RET_OK) {
                JSObject ret = new JSObject();
                ret.put("transactionCode", result.getTransactionCode());
                ret.put("transactionId", result.getTransactionId());
                ret.put("message", result.getMessage());
                return ret;
            } else {
                // Se o resultado não for OK, tentamos o abort por segurança
                plugPagWrapper.abort();
                String code = result.getErrorCode() != null ? result.getErrorCode() : "OPR_ERROR";
                String msg = result.getMessage() != null ? result.getMessage() : "Erro na transação";
                throw new Exception(code + " - " + msg);
            }
        } catch (Exception e) {
            // 2. Se qualquer erro grave acontecer durante o processo, limpa o estado do SDK
            plugPagWrapper.abort();
            throw e;
        }
    }

    public JSObject voidPayment(String transactionCode, String transactionId, boolean printReceipt, PaymentEventListener listener) throws Exception {
        if (plugPagWrapper.isServiceBusy()) {
            throw new Exception("O serviço já está ocupado com outra operação.");
        }

        if (!plugPagWrapper.isAuthenticated()) {
            throw new Exception("POS não autenticado!");
        }

        try {
            PlugPagVoidData voidData = new PlugPagVoidData(transactionCode, transactionId, printReceipt);

            plugPagWrapper.setEventListener(new PlugPagEventListener() {
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
                        msg = data.getCustomMessage() != null ? data.getCustomMessage() : "AGUARDANDO...";
                    }
                    listener.onEvent(msg, data.getEventCode());
                }
            });

            PlugPagTransactionResult result = plugPagWrapper.doVoid(voidData);

            if (result.getResult() == br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag.RET_OK) {
                JSObject ret = new JSObject();
                ret.put("transactionCode", result.getTransactionCode());
                ret.put("transactionId", result.getTransactionId());
                ret.put("message", result.getMessage());
                return ret;
            } else {
                plugPagWrapper.abort();
                String code = result.getErrorCode() != null ? result.getErrorCode() : "OPR_ERROR";
                String msg = result.getMessage() != null ? result.getMessage() : "Erro no cancelamento";
                throw new Exception(code + " - " + msg);
            }
        } catch (Exception e) {
            plugPagWrapper.abort();
            throw e;
        }
    }
}