# capacitor-plugin-plugpag

Plugin Capacitor para integração com terminais de pagamento PagBank via PlugPag SDK.

[![npm version](https://img.shields.io/npm/v/capacitor-plugin-plugpag)](https://www.npmjs.com/package/capacitor-plugin-plugpag)
[![npm downloads](https://img.shields.io/npm/dm/capacitor-plugin-plugpag)](https://www.npmjs.com/package/capacitor-plugin-plugpag)
[![Capacitor](https://img.shields.io/badge/Capacitor-7-blue)](https://capacitorjs.com/)
[![platform](https://img.shields.io/badge/platform-Android-green)](https://developer.android.com/)

## Recursos

- 💳 **Múltiplas formas de pagamento** — Aceite Crédito (à vista ou parcelado), Débito, Voucher e PIX
- 📊 **Consulta de taxas na hora** — Valores de parcelas e totais calculados instantaneamente pelo terminal
- 🔄 **Estorno integrado** — Cancele transações facilmente usando o código ou ID da venda
- 🖨️ **Impressão completa** — Imprima textos, arquivos e PDFs diretamente na impressora térmica da maquininha
- 📡 **Eventos em tempo real** — Acompanhe cada passo do pagamento (ex: "insira o cartão", "digite a senha")
- 🔒 **Operações seguras** — Sistema de fila que evita conflitos e garante que um comando não atropele o outro
- 📘 **TypeScript nativo** — Estrutura moderna e organizada com enums e interfaces

## Instalação

```bash
npm install capacitor-plugin-plugpag
npx cap sync
```

### Requisitos

- Capacitor ≥ 7
- Android API ≥ 21
- Terminal PagBank com PlugPag SDK instalado

## Uso

### Pagamento à vista

```typescript
import { PlugPag, PaymentType, InstallmentType } from 'capacitor-plugin-plugpag';

try {
  const resultado = await PlugPag.doPayment({
    type: PaymentType.CREDIT,
    amount: 2500, // valor em centavos (R$ 25,00)
    installments: 1,
    userReference: 'PED001', // máx. 10 caracteres alfanuméricos
    printReceipt: true,
  });

  console.log('Aprovado:', resultado.transactionCode);
} catch (error) {
  // Transação negada, cancelada ou erro de comunicação.
  // error.message contém o código e a mensagem retornados pelo terminal.
  console.error('Pagamento recusado:', error.message);
}
```

### Crédito parcelado

```typescript
import { PlugPag, PaymentType, InstallmentType } from 'capacitor-plugin-plugpag';

// Parcelado sem juros (lojista absorve) — use BUYER_INSTALLMENT para parcelado com juros
const resultado = await PlugPag.doPayment({
  type: PaymentType.CREDIT,
  amount: 15000, // R$ 150,00
  installmentType: InstallmentType.SELLER_INSTALLMENT,
  installments: 3, // 3x de R$ 50,00
  userReference: 'PED002',
  printReceipt: true,
});
```

### Débito e PIX

```typescript
// Débito — sem parcelamento
await PlugPag.doPayment({
  type: PaymentType.DEBIT,
  amount: 5000,
  userReference: 'PED003',
});

// PIX
await PlugPag.doPayment({
  type: PaymentType.PIX,
  amount: 5000,
  userReference: 'PED004',
});
```

### Acompanhando o progresso em tempo real

Use `addListener` antes ou durante o pagamento para exibir o status ao operador.
O evento `PaymentEvent` carrega `message` (texto para exibição) e `code` (veja `PaymentEventCode`).

```typescript
import { PlugPag, PaymentEventCode } from 'capacitor-plugin-plugpag';

const handle = await PlugPag.addListener('paymentProgress', (event) => {
  console.log(event.message); // ex: "Insira o cartão", "Digite a senha", "Processando..."

  if (event.code === PaymentEventCode.CARD_INSERTED) {
    // cartão físico inserido no terminal
  }
  if (event.code === PaymentEventCode.TRANSACTION_APPROVED) {
    // aprovação confirmada antes do doPayment retornar
  }
});

await PlugPag.doPayment({ ... });

handle.remove(); // sempre remova o listener após a operação
```

### Estorno

O estorno só é possível no mesmo dia da transação. Guarde `transactionCode` e `transactionId` do resultado do pagamento.

```typescript
try {
  await PlugPag.voidPayment({
    transactionCode: resultado.transactionCode,
    transactionId: resultado.transactionId,
    printReceipt: true,
  });
  console.log('Estorno concluído');
} catch (error) {
  console.error('Estorno recusado:', error.message);
}
```

### Impressão

```typescript
// Verificar conexão com o serviço PlugPag antes de imprimir.
// Atenção: este método confirma o IPC com o app PagBank, mas não detecta
// falhas físicas da impressora (papel acabado, etc.).
const { status } = await PlugPag.statusImpressora();

if (status === 'IMPRESSORA OK') {
  // Texto simples — use '\n' para quebrar linhas
  // Referência de largura: size=20 → ~30 chars/linha (padrão 58 mm)
  await PlugPag.imprimirTexto({
    mensagem: 'Obrigado pela compra!\n\n\n',
    size: 20,
  });

  // PDF por URL (renderiza cada página como bitmap 384 px)
  await PlugPag.printPdfFromUrl({ url: 'https://exemplo.com/comprovante.pdf' });

  // Reimprimir o último comprovante do cliente
  await PlugPag.reprintCustomerReceipt();
}
```

### Consultar opções de parcelamento

Use `calculateInstallments` para exibir ao operador as opções reais — com valores por parcela e total — antes de iniciar o pagamento.
O método é bloqueante e consulta o serviço PagBank no terminal.

```typescript
import { PlugPag, InstallmentType } from 'capacitor-plugin-plugpag';

const valorCentavos = Math.round(150.0 * 100); // R$ 150,00 → 15000 centavos

// Parcelado sem juros — lojista absorve as taxas
const { installments: semJuros } = await PlugPag.calculateInstallments({
  value: valorCentavos,
  installmentType: InstallmentType.SELLER_INSTALLMENT,
});

// Parcelado com juros — comprador paga as taxas ao emissor
const { installments: comJuros } = await PlugPag.calculateInstallments({
  value: valorCentavos,
  installmentType: InstallmentType.BUYER_INSTALLMENT,
});

// Cada item de `installments`:
// { installments: 3, installmentValue: 5000, totalValue: 15000 }
//   ↳ todos os valores monetários estão em centavos (divida por 100 para exibir em reais)

for (const op of comJuros) {
  console.log(
    `${op.installments}x de R$ ${(op.installmentValue / 100).toFixed(2)}`,
    `— total R$ ${(op.totalValue / 100).toFixed(2)}`,
  );
}
```

> **Dica:** faça as duas chamadas em paralelo com `Promise.allSettled` para reduzir o tempo de carregamento.

```typescript
const [vendedor, comprador] = await Promise.allSettled([
  PlugPag.calculateInstallments({ value: valorCentavos, installmentType: InstallmentType.SELLER_INSTALLMENT }),
  PlugPag.calculateInstallments({ value: valorCentavos, installmentType: InstallmentType.BUYER_INSTALLMENT }),
]);
```

---

### Cancelar uma operação em andamento

```typescript
// Chame abort() para interromper um doPayment bloqueante.
// O doPayment lançará exceção com mensagem contendo "RET_ABORT".
await PlugPag.abort();
```

### Verificar estado do terminal

```typescript
const { value: autenticado } = await PlugPag.isAuthenticated();
const { value: ocupado } = await PlugPag.isServiceBusy();
```

### Ativar o terminal (primeiro uso)

```typescript
// Necessário apenas na primeira execução ou após reset de fábrica.
await PlugPag.initialize({ activationCode: 'SEU_CODIGO_PAGBANK' });
```

---

## API

<docgen-index>

* [`isAuthenticated()`](#isauthenticated)
* [`isServiceBusy()`](#isservicebusy)
* [`initialize(...)`](#initialize)
* [`doPayment(...)`](#dopayment)
* [`abort()`](#abort)
* [`calculateInstallments(...)`](#calculateinstallments)
* [`voidPayment(...)`](#voidpayment)
* [`imprimirTexto(...)`](#imprimirtexto)
* [`statusImpressora()`](#statusimpressora)
* [`printFromFile(...)`](#printfromfile)
* [`reprintCustomerReceipt()`](#reprintcustomerreceipt)
* [`printPdfFromUrl(...)`](#printpdffromurl)
* [`addListener('paymentProgress', ...)`](#addlistenerpaymentprogress-)
* [`addListener('voidProgress', ...)`](#addlistenervoidprogress-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)
* [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Interface principal do plugin PlugPag para Capacitor.

Permite integrar o terminal de pagamento PagBank Smart (e compatíveis) em aplicações
Ionic/Capacitor via PlugPag SDK Android.

### isAuthenticated()

```typescript
isAuthenticated() => Promise<{ value: boolean; }>
```

Verifica se o terminal está autenticado com o serviço PlugPag (IPC ativo).

**Returns:** <code>Promise&lt;{ value: boolean; }&gt;</code>

--------------------


### isServiceBusy()

```typescript
isServiceBusy() => Promise<{ value: boolean; }>
```

Verifica se o serviço PlugPag está ocupado com uma operação em andamento.

**Returns:** <code>Promise&lt;{ value: boolean; }&gt;</code>

--------------------


### initialize(...)

```typescript
initialize(options: { activationCode: string; }) => Promise<{ status: string; }>
```

Inicializa e ativa o terminal com o código de ativação PagBank.
Necessário apenas na primeira execução ou após reset de fábrica.

| Param         | Type                                     |
| ------------- | ---------------------------------------- |
| **`options`** | <code>{ activationCode: string; }</code> |

**Returns:** <code>Promise&lt;{ status: string; }&gt;</code>

--------------------


### doPayment(...)

```typescript
doPayment(options: { type: PaymentType; amount: number; installmentType?: InstallmentType; installments?: number; userReference: string; printReceipt?: boolean; }) => Promise<PlugPagTransactionResult>
```

Inicia uma cobrança no terminal.

O método é bloqueante até o terminal concluir (aprovado, negado ou cancelado).
Use `addListener('paymentProgress', ...)` para acompanhar o progresso em tempo real.

| Param         | Type                                                                                                                                                                                                                   |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ type: <a href="#paymenttype">PaymentType</a>; amount: number; installmentType?: <a href="#installmenttype">InstallmentType</a>; installments?: number; userReference: string; printReceipt?: boolean; }</code> |

**Returns:** <code>Promise&lt;<a href="#plugpagtransactionresult">PlugPagTransactionResult</a>&gt;</code>

--------------------


### abort()

```typescript
abort() => Promise<{ result: ErrorCode; }>
```

Aborta a operação em andamento (pagamento ou estorno).
Retorna imediatamente — o `doPayment` ou `voidPayment` falhará com código `OPERATION_ABORTED (-1)`.

**Returns:** <code>Promise&lt;{ result: <a href="#errorcode">ErrorCode</a>; }&gt;</code>

--------------------


### calculateInstallments(...)

```typescript
calculateInstallments(options: { value: number; installmentType: InstallmentType; }) => Promise<{ installments: PlugPagInstallment[]; }>
```

Consulta as opções de parcelamento para um valor e modalidade.

Operação bloqueante — o SDK consulta o serviço PagBank e retorna os valores reais
calculados com base no plano de recebimento do lojista vinculado ao terminal.

| Param         | Type                                                                                             |
| ------------- | ------------------------------------------------------------------------------------------------ |
| **`options`** | <code>{ value: number; installmentType: <a href="#installmenttype">InstallmentType</a>; }</code> |

**Returns:** <code>Promise&lt;{ installments: PlugPagInstallment[]; }&gt;</code>

--------------------


### voidPayment(...)

```typescript
voidPayment(options: { transactionCode: string; transactionId: string; printReceipt?: boolean; }) => Promise<PlugPagTransactionResult>
```

Estorna (cancela) uma transação previamente aprovada.

Só é possível no mesmo dia da transação original.
Use `addListener('voidProgress', ...)` para acompanhar o progresso.

| Param         | Type                                                                                     |
| ------------- | ---------------------------------------------------------------------------------------- |
| **`options`** | <code>{ transactionCode: string; transactionId: string; printReceipt?: boolean; }</code> |

**Returns:** <code>Promise&lt;<a href="#plugpagtransactionresult">PlugPagTransactionResult</a>&gt;</code>

--------------------


### imprimirTexto(...)

```typescript
imprimirTexto(options: { mensagem: string; size?: number; }) => Promise<void>
```

Imprime texto diretamente na impressora térmica do terminal (58 mm / 384 px).

O texto é renderizado em um bitmap monoespaçado e enviado via PlugPag SDK.
Use quebras de linha (`\n`) para múltiplas linhas.

**Referência de largura por tamanho de fonte:**
| `size` | chars/linha aprox. |
|--------|--------------------|
| 18     | ~34                |
| 20     | ~30 (recomendado)  |
| 26     | ~23                |

| Param         | Type                                              |
| ------------- | ------------------------------------------------- |
| **`options`** | <code>{ mensagem: string; size?: number; }</code> |

--------------------


### statusImpressora()

```typescript
statusImpressora() => Promise<{ status: string; }>
```

Verifica se o serviço PlugPag está autenticado e acessível.

**Atenção:** este método usa `isAuthenticated()` como proxy — confirma que o IPC
com o app PagBank está ativo, mas **não detecta falhas físicas** da impressora
(papel acabado, cabeçote com defeito, etc.). Erros físicos são reportados apenas
como resultado de uma tentativa de impressão.

**Returns:** <code>Promise&lt;{ status: string; }&gt;</code>

--------------------


### printFromFile(...)

```typescript
printFromFile(options: { filePath: string; }) => Promise<void>
```

Imprime a partir de um arquivo de imagem já salvo no dispositivo.

| Param         | Type                               |
| ------------- | ---------------------------------- |
| **`options`** | <code>{ filePath: string; }</code> |

--------------------


### reprintCustomerReceipt()

```typescript
reprintCustomerReceipt() => Promise<void>
```

Reimprimir o último comprovante do cliente diretamente pelo terminal.

--------------------


### printPdfFromUrl(...)

```typescript
printPdfFromUrl(options: { url: string; }) => Promise<void>
```

Baixa um PDF da URL informada, renderiza cada página como bitmap (384 px de largura)
e imprime via PlugPag SDK página a página.

Em dispositivos Android mais antigos, um `TrustManager` permissivo é aplicado
apenas para esta conexão HTTPS — valide o certificado do servidor antes de usar em produção.

| Param         | Type                          |
| ------------- | ----------------------------- |
| **`options`** | <code>{ url: string; }</code> |

--------------------


### addListener('paymentProgress', ...)

```typescript
addListener(eventName: 'paymentProgress', listenerFunc: (info: PaymentEvent) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Escuta eventos de progresso durante {@link doPayment}.

| Param              | Type                                                                     |
| ------------------ | ------------------------------------------------------------------------ |
| **`eventName`**    | <code>'paymentProgress'</code>                                           |
| **`listenerFunc`** | <code>(info: <a href="#paymentevent">PaymentEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### addListener('voidProgress', ...)

```typescript
addListener(eventName: 'voidProgress', listenerFunc: (info: PaymentEvent) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Escuta eventos de progresso durante {@link voidPayment}.

| Param              | Type                                                                     |
| ------------------ | ------------------------------------------------------------------------ |
| **`eventName`**    | <code>'voidProgress'</code>                                              |
| **`listenerFunc`** | <code>(info: <a href="#paymentevent">PaymentEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Remove todos os listeners registrados neste plugin.
Chame no `ngOnDestroy` ou equivalente para evitar memory leaks.

--------------------


### Interfaces


#### PlugPagTransactionResult

Resultado completo de uma transação (pagamento ou estorno).
Todos os campos opcionais podem ser `undefined` dependendo da bandeira e do tipo de transação.

| Prop                       | Type                | Description                                                                                  |
| -------------------------- | ------------------- | -------------------------------------------------------------------------------------------- |
| **`transactionCode`**      | <code>string</code> | Código interno da transação — necessário para estorno via {@link PlugPagPlugin.voidPayment}. |
| **`transactionId`**        | <code>string</code> | ID externo da transação — necessário para estorno via {@link PlugPagPlugin.voidPayment}.     |
| **`message`**              | <code>string</code> | Mensagem de resposta do host autorizador.                                                    |
| **`errorCode`**            | <code>string</code> | Código de erro do SDK (presente apenas em casos de falha tratada).                           |
| **`hostNsu`**              | <code>string</code> | NSU do host autorizador.                                                                     |
| **`date`**                 | <code>string</code> | Data da transação no formato retornado pelo terminal (DDMMAA).                               |
| **`time`**                 | <code>string</code> | Hora da transação no formato retornado pelo terminal (HHmmss).                               |
| **`cardBrand`**            | <code>string</code> | Bandeira do cartão (ex: "VISA", "MASTERCARD").                                               |
| **`bin`**                  | <code>string</code> | Seis primeiros dígitos do cartão (BIN).                                                      |
| **`holder`**               | <code>string</code> | Nome do portador do cartão conforme gravado na trilha.                                       |
| **`userReference`**        | <code>string</code> | Referência enviada no momento do pagamento via `userReference`.                              |
| **`terminalSerialNumber`** | <code>string</code> | Número de série do terminal PagBank.                                                         |
| **`amount`**               | <code>string</code> | Valor cobrado em centavos como string (ex: `"15000"` para R$ 150,00).                        |
| **`availableBalance`**     | <code>string</code> | Saldo disponível (para vouchers/pré-pagos).                                                  |
| **`cardApplication`**      | <code>string</code> | Aplicação EMV selecionada pelo cartão.                                                       |
| **`label`**                | <code>string</code> | Label da aplicação EMV.                                                                      |
| **`holderName`**           | <code>string</code> | Nome do portador (campo curto).                                                              |
| **`extendedHolderName`**   | <code>string</code> | Nome do portador (campo estendido, quando disponível).                                       |
| **`installments`**         | <code>string</code> | Número de parcelas confirmadas pelo terminal como string (ex: `"3"`).                        |


#### PlugPagInstallment

Uma opção de parcelamento retornada por {@link PlugPagPlugin.calculateInstallments}.
Os valores monetários estão em centavos (divida por 100 para exibir em reais).

| Prop                   | Type                | Description                                                |
| ---------------------- | ------------------- | ---------------------------------------------------------- |
| **`installments`**     | <code>number</code> | Número de parcelas (ex: `3` para 3x).                      |
| **`installmentValue`** | <code>number</code> | Valor de cada parcela em centavos (ex: `5670` = R$ 56,70). |
| **`totalValue`**       | <code>number</code> | Valor total com juros em centavos.                         |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### PaymentEvent

Evento de progresso emitido pelo terminal durante um pagamento ou estorno.
Recebido via listener `paymentProgress` ou `voidProgress`.

| Prop          | Type                                                          | Description                                                                                |
| ------------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| **`code`**    | <code><a href="#paymenteventcode">PaymentEventCode</a></code> | Código numérico do evento (veja {@link <a href="#paymenteventcode">PaymentEventCode</a>}). |
| **`message`** | <code>string</code>                                           | Mensagem legível para exibição ao operador.                                                |


### Enums


#### PaymentType

| Members       | Value          |
| ------------- | -------------- |
| **`CREDIT`**  | <code>1</code> |
| **`DEBIT`**   | <code>2</code> |
| **`VOUCHER`** | <code>3</code> |
| **`PIX`**     | <code>5</code> |


#### InstallmentType

| Members                  | Value          |
| ------------------------ | -------------- |
| **`NO_INSTALLMENT`**     | <code>1</code> |
| **`SELLER_INSTALLMENT`** | <code>2</code> |
| **`BUYER_INSTALLMENT`**  | <code>3</code> |


#### ErrorCode

| Members                     | Value           |
| --------------------------- | --------------- |
| **`OK`**                    | <code>0</code>  |
| **`OPERATION_ABORTED`**     | <code>-1</code> |
| **`AUTHENTICATION_FAILED`** | <code>-2</code> |
| **`COMMUNICATION_ERROR`**   | <code>-3</code> |
| **`NO_PRINTER_DEVICE`**     | <code>-4</code> |
| **`NO_TRANSACTION_DATA`**   | <code>-5</code> |


#### PaymentEventCode

| Members                      | Value             |
| ---------------------------- | ----------------- |
| **`CARD_INSERTED`**          | <code>1001</code> |
| **`CARD_REMOVED`**           | <code>1002</code> |
| **`CARD_TAPPED`**            | <code>1003</code> |
| **`WAITING_CARD`**           | <code>1004</code> |
| **`DIGIT_PASSWORD`**         | <code>1010</code> |
| **`NO_PASSWORD`**            | <code>1011</code> |
| **`LAST_PASSWORD_TRY`**      | <code>1012</code> |
| **`PROCESSING_TRANSACTION`** | <code>1020</code> |
| **`CONNECTING_TO_NETWORK`**  | <code>1021</code> |
| **`SENDING_DATA`**           | <code>1022</code> |
| **`WAITING_HOST_RESPONSE`**  | <code>1023</code> |
| **`REMOVE_CARD`**            | <code>1030</code> |
| **`TRANSACTION_APPROVED`**   | <code>1031</code> |
| **`TRANSACTION_DENIED`**     | <code>1032</code> |
| **`COMMUNICATION_ERROR`**    | <code>1040</code> |
| **`INVALID_CARD`**           | <code>1041</code> |
| **`CARD_BLOCKED`**           | <code>1042</code> |
| **`INSUFFICIENT_FUNDS`**     | <code>1043</code> |
| **`TRANSACTION_CANCELLED`**  | <code>1050</code> |
| **`SIGNATURE_REQUIRED`**     | <code>1051</code> |
| **`PRINTING_RECEIPT`**       | <code>1052</code> |

</docgen-api>
