import type { PluginListenerHandle } from '@capacitor/core';

export interface PlugPagPlugin {
  /**
   * Inicializa e ativa o terminal
   */
  initialize(options: { activationCode: string }): Promise<{ status: string }>;

  /**
   * Realiza um pagamento
   */
  doPayment(options: {
    type: number;
    amount: number;
    installmentType?: number;
    installments?: number;
    userReference?: string;
    printReceipt?: boolean;
  }): Promise<any>;

  /**
   * Escuta eventos da maquininha (mensagens de senha, processamento, etc)
   */
  addListener(
    eventName: 'paymentProgress',
    listenerFunc: (info: { message: string; code: number }) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Remove todos os listeners
   */
  removeAllListeners(): Promise<void>;
}
