import type { PluginListenerHandle } from '@capacitor/core';

export interface PlugPagPlugin {
  isAuthenticated(): Promise<{ value: boolean }>;
  isServiceBusy(): Promise<{ value: boolean }>;
  initialize(options: { activationCode: string }): Promise<{ status: string }>;

  doPayment(options: {
    type: number;
    amount: number;
    installmentType?: number;
    installments?: number;
    userReference: string;
    printReceipt?: boolean;
  }): Promise<{ transactionCode: string; transactionId: string; message: string }>;

  abort(): Promise<{ result: number }>;

  voidPayment(options: {
    transactionCode: string;
    transactionId: string;
    printReceipt?: boolean;
  }): Promise<{ transactionCode: string; transactionId: string; message: string }>;

  /**
   * Escuta o progresso do pagamento (mensagens como 'Insira o cartão', 'Senha', etc)
   */
  addListener(
    eventName: 'paymentProgress',
    listenerFunc: (info: { message: string; code: number }) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Escuta o progresso do estorno
   */
  addListener(
    eventName: 'voidProgress',
    listenerFunc: (info: { message: string; code: number }) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Remove todos os listeners ativos
   */
  removeAllListeners(): Promise<void>;
}
