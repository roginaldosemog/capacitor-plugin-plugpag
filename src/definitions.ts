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
   * Imprime texto diretamente via PlugPag SDK.
   * Compatível com a interface do plugin Gpos.
   */
  imprimirTexto(options: {
    mensagem: string;
    alinhar?: string;
    size?: number;
  }): Promise<void>;

  /**
   * Verifica o status da impressora.
   * Compatível com a interface do plugin Gpos.
   */
  statusImpressora(): Promise<{ status: string }>;

  /**
   * Imprime a partir de um arquivo no dispositivo.
   * @param filePath Caminho absoluto do arquivo a imprimir
   */
  printFromFile(options: { filePath: string }): Promise<{ result: number }>;

  /**
   * Reimprimir o último comprovante do cliente.
   */
  reprintCustomerReceipt(): Promise<void>;

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
