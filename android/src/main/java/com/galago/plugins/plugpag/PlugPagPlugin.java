package com.galago.plugins.plugpag;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "PlugPag")
public class PlugPagPlugin extends Plugin {

    private PlugPag implementation;

    @Override
    public void load() {
        implementation = new PlugPag();
        // Passamos o contexto do Android para o SDK do PagSeguro
        implementation.initializeWrapper(getContext());
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        String activationCode = call.getString("activationCode");

        if (activationCode == null) {
            call.reject("Código de ativação é obrigatório.");
            return;
        }

        // Executa em background para não travar o App
        getBridge().execute(() -> {
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
        Integer type = call.getInt("type", 1); // Ex: 1 = Crédito, 2 = Débito
        Integer amount = call.getInt("amount"); // Em centavos!
        Integer installmentType = call.getInt("installmentType", 1);
        Integer installments = call.getInt("installments", 1);
        String userReference = call.getString("userReference", "");
        Boolean printReceipt = call.getBoolean("printReceipt", true);

        if (amount == null) {
            call.reject("O valor (amount) é obrigatório.");
            return;
        }

        getBridge().execute(() -> {
            try {
                JSObject result = implementation.doPayment(
                    type, amount, installmentType, installments, userReference, printReceipt,
                    
                    // Listener de Eventos (Isso manda as mensagens "Insira o Cartão" pro JS)
                    (message, eventCode) -> {
                        JSObject eventData = new JSObject();
                        eventData.put("message", message);
                        eventData.put("code", eventCode);
                        notifyListeners("paymentProgress", eventData);
                    }
                );
                
                // Pagamento aprovado!
                call.resolve(result);
            } catch (Exception e) {
                // Pagamento negado ou erro
                call.reject(e.getMessage());
            }
        });
    }
}