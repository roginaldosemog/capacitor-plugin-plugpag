import type { PluginListenerHandle } from '@capacitor/core';

export enum PaymentType {
  CREDIT = 1,
  DEBIT = 2,
  VOUCHER = 3,
  PIX = 5,
}

export enum InstallmentType {
  NO_INSTALLMENT = 1,
  SELLER_INSTALLMENT = 2,
  BUYER_INSTALLMENT = 3,
}

export enum ErrorCode {
  OK = 0,
  OPERATION_ABORTED = -1,
  AUTHENTICATION_FAILED = -2,
  COMMUNICATION_ERROR = -3,
  NO_PRINTER_DEVICE = -4,
  NO_TRANSACTION_DATA = -5,
}

export enum ActionType {
  POST_OPERATION = 1,
  PRE_OPERATION = 2,
  UPDATE = 3,
}

export enum PaymentEventCode {
  // Eventos de leitura do cartão
  CARD_INSERTED = 1001,
  CARD_REMOVED = 1002,
  CARD_TAPPED = 1003,
  WAITING_CARD = 1004,

  // Eventos de senha
  DIGIT_PASSWORD = 1010,
  NO_PASSWORD = 1011,
  LAST_PASSWORD_TRY = 1012,

  // Eventos de processamento
  PROCESSING_TRANSACTION = 1020,
  CONNECTING_TO_NETWORK = 1021,
  SENDING_DATA = 1022,
  WAITING_HOST_RESPONSE = 1023,

  // Eventos do terminal
  REMOVE_CARD = 1030,
  TRANSACTION_APPROVED = 1031,
  TRANSACTION_DENIED = 1032,

  // Eventos de erro
  COMMUNICATION_ERROR = 1040,
  INVALID_CARD = 1041,
  CARD_BLOCKED = 1042,
  INSUFFICIENT_FUNDS = 1043,

  // Outros eventos
  TRANSACTION_CANCELLED = 1050,
  SIGNATURE_REQUIRED = 1051,
  PRINTING_RECEIPT = 1052,
}

export interface PaymentEvent {
  code: PaymentEventCode;
  message: string;
}

export interface PlugPagTransactionResult {
  transactionCode: string;
  transactionId: string;
  message: string;
  errorCode?: string;
  hostNsu?: string;
  date?: string;
  time?: string;
  cardBrand?: string;
  bin?: string;
  holder?: string;
  userReference?: string;
  terminalSerialNumber?: string;
  amount?: string;
  availableBalance?: string;
  cardApplication?: string;
  label?: string;
  holderName?: string;
  extendedHolderName?: string;
  installments?: string;
}

export interface PlugPagPlugin {
  isAuthenticated(): Promise<{ value: boolean }>;
  isServiceBusy(): Promise<{ value: boolean }>;
  initialize(options: { activationCode: string }): Promise<{ status: string }>;

  doPayment(options: {
    type: PaymentType;
    amount: number;
    installmentType?: InstallmentType;
    installments?: number;
    userReference: string;
    printReceipt?: boolean;
  }): Promise<PlugPagTransactionResult>;

  abort(): Promise<{ result: ErrorCode }>;

  voidPayment(options: {
    transactionCode: string;
    transactionId: string;
    printReceipt?: boolean;
  }): Promise<PlugPagTransactionResult>;

  /**
   * Imprime texto diretamente via PlugPag SDK.
   */
  imprimirTexto(options: {
    mensagem: string;
    alinhar?: string;
    size?: number;
  }): Promise<void>;

  /**
   * Verifica o status da impressora.
   */
  statusImpressora(): Promise<{ status: string }>;

  /**
   * Imprime a partir de um arquivo no dispositivo.
   * @param filePath Caminho absoluto do arquivo a imprimir
   */
  printFromFile(options: { filePath: string }): Promise<{ result: ErrorCode }>;

  /**
   * Reimprimir o último comprovante do cliente.
   */
  reprintCustomerReceipt(): Promise<void>;

  /**
   * Baixa um PDF da URL informada, renderiza cada página como bitmap e imprime via PlugPag SDK.
   */
  printPdfFromUrl(options: { url: string }): Promise<void>;

  /**
   * Escuta o progresso do pagamento (mensagens como 'Insira o cartão', 'Senha', etc)
   */
  addListener(
    eventName: 'paymentProgress',
    listenerFunc: (info: PaymentEvent) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Escuta o progresso do estorno
   */
  addListener(
    eventName: 'voidProgress',
    listenerFunc: (info: PaymentEvent) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Remove todos os listeners ativos
   */
  removeAllListeners(): Promise<void>;
}
