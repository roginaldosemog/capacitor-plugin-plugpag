import type { PluginListenerHandle } from '@capacitor/core';

/**
 * Tipo de pagamento suportado pelo terminal PlugPag.
 *
 * Os valores numéricos correspondem às constantes do SDK Android PagBank PlugPag
 * (`br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag`).
 */
export enum PaymentType {
  CREDIT  = 1,
  DEBIT   = 2,
  VOUCHER = 3,
  PIX     = 5,
}

/**
 * Modalidade de parcelamento para pagamentos no crédito.
 *
 * - `NO_INSTALLMENT`   — à vista (1 parcela). Use sempre que `installments === 1`.
 * - `SELLER_INSTALLMENT` — parcelado sem juros (lojista absorve).
 * - `BUYER_INSTALLMENT`  — parcelado com juros (comprador paga juros ao emissor).
 *
 * Para débito e PIX use sempre `NO_INSTALLMENT`.
 */
export enum InstallmentType {
  NO_INSTALLMENT     = 1,
  SELLER_INSTALLMENT = 2,
  BUYER_INSTALLMENT  = 3,
}

/**
 * Códigos de retorno de operações do plugin.
 * `OK` indica sucesso; os demais indicam falha ou interrupção da operação.
 */
export enum ErrorCode {
  OK                   =  0,
  OPERATION_ABORTED    = -1,
  AUTHENTICATION_FAILED = -2,
  COMMUNICATION_ERROR  = -3,
  NO_PRINTER_DEVICE    = -4,
  NO_TRANSACTION_DATA  = -5,
}

/**
 * Fase da operação no terminal. Faz parte do SDK PlugPag e é exportado para referência;
 * não é utilizado diretamente pelos métodos deste plugin.
 */
export enum ActionType {
  POST_OPERATION = 1,
  PRE_OPERATION  = 2,
  UPDATE         = 3,
}

/**
 * Códigos de evento emitidos durante `paymentProgress` / `voidProgress`.
 *
 * Os valores numéricos são extraídos das constantes `EVENT_CODE_*` de
 * `PlugPagEventData` do SDK Android. Verifique a documentação oficial do PagBank
 * caso precise garantir compatibilidade com versões futuras do wrapper.
 */
export enum PaymentEventCode {
  // Leitura do cartão
  CARD_INSERTED  = 1001,
  CARD_REMOVED   = 1002,
  CARD_TAPPED    = 1003,
  WAITING_CARD   = 1004,

  // Senha
  DIGIT_PASSWORD    = 1010,
  NO_PASSWORD       = 1011,
  LAST_PASSWORD_TRY = 1012,

  // Processamento
  PROCESSING_TRANSACTION = 1020,
  CONNECTING_TO_NETWORK  = 1021,
  SENDING_DATA           = 1022,
  WAITING_HOST_RESPONSE  = 1023,

  // Resultado no terminal
  REMOVE_CARD          = 1030,
  TRANSACTION_APPROVED = 1031,
  TRANSACTION_DENIED   = 1032,

  // Erros
  COMMUNICATION_ERROR = 1040,
  INVALID_CARD        = 1041,
  CARD_BLOCKED        = 1042,
  INSUFFICIENT_FUNDS  = 1043,

  // Outros
  TRANSACTION_CANCELLED = 1050,
  SIGNATURE_REQUIRED    = 1051,
  PRINTING_RECEIPT      = 1052,
}

/**
 * Evento de progresso emitido pelo terminal durante um pagamento ou estorno.
 * Recebido via listener `paymentProgress` ou `voidProgress`.
 */
export interface PaymentEvent {
  /** Código numérico do evento (veja {@link PaymentEventCode}). */
  code: PaymentEventCode;
  /** Mensagem legível para exibição ao operador. */
  message: string;
}

/**
 * Resultado completo de uma transação (pagamento ou estorno).
 * Todos os campos opcionais podem ser `undefined` dependendo da bandeira e do tipo de transação.
 */
export interface PlugPagTransactionResult {
  /** Código interno da transação — necessário para estorno via {@link PlugPagPlugin.voidPayment}. */
  transactionCode: string;
  /** ID externo da transação — necessário para estorno via {@link PlugPagPlugin.voidPayment}. */
  transactionId: string;
  /** Mensagem de resposta do host autorizador. */
  message: string;
  /** Código de erro do SDK (presente apenas em casos de falha tratada). */
  errorCode?: string;
  /** NSU do host autorizador. */
  hostNsu?: string;
  /** Data da transação no formato retornado pelo terminal (DDMMAA). */
  date?: string;
  /** Hora da transação no formato retornado pelo terminal (HHmmss). */
  time?: string;
  /** Bandeira do cartão (ex: "VISA", "MASTERCARD"). */
  cardBrand?: string;
  /** Seis primeiros dígitos do cartão (BIN). */
  bin?: string;
  /** Nome do portador do cartão conforme gravado na trilha. */
  holder?: string;
  /** Referência enviada no momento do pagamento via `userReference`. */
  userReference?: string;
  /** Número de série do terminal PagBank. */
  terminalSerialNumber?: string;
  /** Valor cobrado em centavos como string (ex: `"15000"` para R$ 150,00). */
  amount?: string;
  /** Saldo disponível (para vouchers/pré-pagos). */
  availableBalance?: string;
  /** Aplicação EMV selecionada pelo cartão. */
  cardApplication?: string;
  /** Label da aplicação EMV. */
  label?: string;
  /** Nome do portador (campo curto). */
  holderName?: string;
  /** Nome do portador (campo estendido, quando disponível). */
  extendedHolderName?: string;
  /** Número de parcelas confirmadas pelo terminal como string (ex: `"3"`). */
  installments?: string;
}

/**
 * Interface principal do plugin PlugPag para Capacitor.
 *
 * Permite integrar o terminal de pagamento PagBank Smart (e compatíveis) em aplicações
 * Ionic/Capacitor via PlugPag SDK Android.
 *
 * @example
 * ```typescript
 * import { PlugPag, PaymentType, InstallmentType } from 'capacitor-plugin-plugpag';
 *
 * const result = await PlugPag.doPayment({
 *   type: PaymentType.CREDIT,
 *   amount: 15000,                              // R$ 150,00 em centavos
 *   installmentType: InstallmentType.SELLER_INSTALLMENT,
 *   installments: 3,
 *   userReference: 'PEDIDO001',
 *   printReceipt: true,
 * });
 * ```
 */
export interface PlugPagPlugin {
  /**
   * Verifica se o terminal está autenticado com o serviço PlugPag (IPC ativo).
   */
  isAuthenticated(): Promise<{ value: boolean }>;

  /**
   * Verifica se o serviço PlugPag está ocupado com uma operação em andamento.
   */
  isServiceBusy(): Promise<{ value: boolean }>;

  /**
   * Inicializa e ativa o terminal com o código de ativação PagBank.
   * Necessário apenas na primeira execução ou após reset de fábrica.
   *
   * @param options.activationCode Código fornecido pelo PagBank para ativação do POS.
   * @throws Se a ativação falhar (código + mensagem do SDK no erro).
   */
  initialize(options: { activationCode: string }): Promise<{ status: string }>;

  /**
   * Inicia uma cobrança no terminal.
   *
   * O método é bloqueante até o terminal concluir (aprovado, negado ou cancelado).
   * Use `addListener('paymentProgress', ...)` para acompanhar o progresso em tempo real.
   *
   * @param options.type        Tipo de pagamento ({@link PaymentType}).
   * @param options.amount      Valor **em centavos** (ex: `15000` para R$ 150,00).
   * @param options.installmentType Modalidade de parcelamento ({@link InstallmentType}).
   *                            Obrigatório quando `installments > 1`.
   * @param options.installments Número de parcelas (padrão: `1`).
   * @param options.userReference Referência livre (max 10 caracteres alfanuméricos) para
   *                              rastreamento no extrato PagBank.
   * @param options.printReceipt Se `true`, o terminal imprime o comprovante automaticamente (padrão: `true`).
   * @throws Se a transação for recusada ou abortada (mensagem do SDK no erro).
   */
  doPayment(options: {
    type: PaymentType;
    amount: number;
    installmentType?: InstallmentType;
    installments?: number;
    userReference: string;
    printReceipt?: boolean;
  }): Promise<PlugPagTransactionResult>;

  /**
   * Aborta a operação de pagamento em andamento.
   * Retorna imediatamente — o `doPayment` falhará com código `OPERATION_ABORTED (-1)`.
   */
  abort(): Promise<{ result: ErrorCode }>;

  /**
   * Estorna (cancela) uma transação previamente aprovada.
   *
   * Só é possível no mesmo dia da transação original.
   * Use `addListener('voidProgress', ...)` para acompanhar o progresso.
   *
   * @param options.transactionCode Obtido em {@link PlugPagTransactionResult.transactionCode}.
   * @param options.transactionId   Obtido em {@link PlugPagTransactionResult.transactionId}.
   * @param options.printReceipt    Se `true`, imprime o comprovante de cancelamento (padrão: `true`).
   * @throws Se o estorno for recusado ou o terminal não estiver autenticado.
   */
  voidPayment(options: {
    transactionCode: string;
    transactionId: string;
    printReceipt?: boolean;
  }): Promise<PlugPagTransactionResult>;

  /**
   * Imprime texto diretamente na impressora térmica do terminal (58 mm / 384 px).
   *
   * O texto é renderizado em um bitmap monoespaçado e enviado via PlugPag SDK.
   * Use quebras de linha (`\n`) para múltiplas linhas.
   *
   * **Referência de largura por tamanho de fonte:**
   * | `size` | chars/linha aprox. |
   * |--------|--------------------|
   * | 18     | ~34                |
   * | 20     | ~30 (recomendado)  |
   * | 26     | ~23                |
   *
   * @param options.mensagem Texto a imprimir (use `\n` para quebrar linhas).
   * @param options.size     Tamanho da fonte em pontos (padrão: `20`).
   */
  imprimirTexto(options: {
    mensagem: string;
    size?: number;
  }): Promise<void>;

  /**
   * Verifica se o serviço PlugPag está autenticado e acessível.
   *
   * **Atenção:** este método usa `isAuthenticated()` como proxy — confirma que o IPC
   * com o app PagBank está ativo, mas **não detecta falhas físicas** da impressora
   * (papel acabado, cabeçote com defeito, etc.). Erros físicos são reportados apenas
   * como resultado de uma tentativa de impressão.
   *
   * @returns `{ status: "IMPRESSORA OK" }` se autenticado, `{ status: "TERMINAL NAO AUTENTICADO" }` caso contrário.
   */
  statusImpressora(): Promise<{ status: string }>;

  /**
   * Imprime a partir de um arquivo de imagem já salvo no dispositivo.
   *
   * @param options.filePath Caminho absoluto do arquivo (JPEG/PNG recomendado, largura 384 px).
   */
  printFromFile(options: { filePath: string }): Promise<{ result: ErrorCode }>;

  /**
   * Reimprimir o último comprovante do cliente diretamente pelo terminal.
   */
  reprintCustomerReceipt(): Promise<void>;

  /**
   * Baixa um PDF da URL informada, renderiza cada página como bitmap (384 px de largura)
   * e imprime via PlugPag SDK página a página.
   *
   * Em dispositivos Android mais antigos, um `TrustManager` permissivo é aplicado
   * apenas para esta conexão HTTPS — valide o certificado do servidor antes de usar em produção.
   *
   * @param options.url URL pública do arquivo PDF.
   */
  printPdfFromUrl(options: { url: string }): Promise<void>;

  /**
   * Escuta eventos de progresso durante {@link doPayment}.
   *
   * @example
   * ```typescript
   * const handle = await PlugPag.addListener('paymentProgress', (event) => {
   *   console.log(event.code, event.message);
   * });
   * // remover quando não precisar mais:
   * handle.remove();
   * ```
   */
  addListener(
    eventName: 'paymentProgress',
    listenerFunc: (info: PaymentEvent) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Escuta eventos de progresso durante {@link voidPayment}.
   */
  addListener(
    eventName: 'voidProgress',
    listenerFunc: (info: PaymentEvent) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Remove todos os listeners registrados neste plugin.
   * Chame no `ngOnDestroy` ou equivalente para evitar memory leaks.
   */
  removeAllListeners(): Promise<void>;
}
