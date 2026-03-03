# capacitor-plugin-plugpag

Plugin for PagSeguro Maquininha's PlugPag SDK

## Install

```bash
npm install capacitor-plugin-plugpag
npx cap sync
```

## API

<docgen-index>

* [`isAuthenticated()`](#isauthenticated)
* [`isServiceBusy()`](#isservicebusy)
* [`initialize(...)`](#initialize)
* [`doPayment(...)`](#dopayment)
* [`abort()`](#abort)
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

### isAuthenticated()

```typescript
isAuthenticated() => Promise<{ value: boolean; }>
```

**Returns:** <code>Promise&lt;{ value: boolean; }&gt;</code>

--------------------


### isServiceBusy()

```typescript
isServiceBusy() => Promise<{ value: boolean; }>
```

**Returns:** <code>Promise&lt;{ value: boolean; }&gt;</code>

--------------------


### initialize(...)

```typescript
initialize(options: { activationCode: string; }) => Promise<{ status: string; }>
```

| Param         | Type                                     |
| ------------- | ---------------------------------------- |
| **`options`** | <code>{ activationCode: string; }</code> |

**Returns:** <code>Promise&lt;{ status: string; }&gt;</code>

--------------------


### doPayment(...)

```typescript
doPayment(options: { type: PaymentType; amount: number; installmentType?: InstallmentType; installments?: number; userReference: string; printReceipt?: boolean; }) => Promise<PlugPagTransactionResult>
```

| Param         | Type                                                                                                                                                                                                                   |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ type: <a href="#paymenttype">PaymentType</a>; amount: number; installmentType?: <a href="#installmenttype">InstallmentType</a>; installments?: number; userReference: string; printReceipt?: boolean; }</code> |

**Returns:** <code>Promise&lt;<a href="#plugpagtransactionresult">PlugPagTransactionResult</a>&gt;</code>

--------------------


### abort()

```typescript
abort() => Promise<{ result: ErrorCode; }>
```

**Returns:** <code>Promise&lt;{ result: <a href="#errorcode">ErrorCode</a>; }&gt;</code>

--------------------


### voidPayment(...)

```typescript
voidPayment(options: { transactionCode: string; transactionId: string; printReceipt?: boolean; }) => Promise<PlugPagTransactionResult>
```

| Param         | Type                                                                                     |
| ------------- | ---------------------------------------------------------------------------------------- |
| **`options`** | <code>{ transactionCode: string; transactionId: string; printReceipt?: boolean; }</code> |

**Returns:** <code>Promise&lt;<a href="#plugpagtransactionresult">PlugPagTransactionResult</a>&gt;</code>

--------------------


### imprimirTexto(...)

```typescript
imprimirTexto(options: { mensagem: string; alinhar?: string; size?: number; }) => Promise<void>
```

Imprime texto diretamente via PlugPag SDK.
Compatível com a interface do plugin Gpos.

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code>{ mensagem: string; alinhar?: string; size?: number; }</code> |

--------------------


### statusImpressora()

```typescript
statusImpressora() => Promise<{ status: string; }>
```

Verifica o status da impressora.
Compatível com a interface do plugin Gpos.

**Returns:** <code>Promise&lt;{ status: string; }&gt;</code>

--------------------


### printFromFile(...)

```typescript
printFromFile(options: { filePath: string; }) => Promise<{ result: ErrorCode; }>
```

Imprime a partir de um arquivo no dispositivo.

| Param         | Type                               |
| ------------- | ---------------------------------- |
| **`options`** | <code>{ filePath: string; }</code> |

**Returns:** <code>Promise&lt;{ result: <a href="#errorcode">ErrorCode</a>; }&gt;</code>

--------------------


### reprintCustomerReceipt()

```typescript
reprintCustomerReceipt() => Promise<void>
```

Reimprimir o último comprovante do cliente.

--------------------


### printPdfFromUrl(...)

```typescript
printPdfFromUrl(options: { url: string; }) => Promise<void>
```

Baixa um PDF da URL informada, renderiza cada página como bitmap e imprime via PlugPag SDK.

| Param         | Type                          |
| ------------- | ----------------------------- |
| **`options`** | <code>{ url: string; }</code> |

--------------------


### addListener('paymentProgress', ...)

```typescript
addListener(eventName: 'paymentProgress', listenerFunc: (info: PaymentEvent) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Escuta o progresso do pagamento (mensagens como 'Insira o cartão', 'Senha', etc)

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

Escuta o progresso do estorno

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

Remove todos os listeners ativos

--------------------


### Interfaces


#### PlugPagTransactionResult

| Prop                       | Type                |
| -------------------------- | ------------------- |
| **`transactionCode`**      | <code>string</code> |
| **`transactionId`**        | <code>string</code> |
| **`message`**              | <code>string</code> |
| **`errorCode`**            | <code>string</code> |
| **`hostNsu`**              | <code>string</code> |
| **`date`**                 | <code>string</code> |
| **`time`**                 | <code>string</code> |
| **`cardBrand`**            | <code>string</code> |
| **`bin`**                  | <code>string</code> |
| **`holder`**               | <code>string</code> |
| **`userReference`**        | <code>string</code> |
| **`terminalSerialNumber`** | <code>string</code> |
| **`amount`**               | <code>string</code> |
| **`availableBalance`**     | <code>string</code> |
| **`cardApplication`**      | <code>string</code> |
| **`label`**                | <code>string</code> |
| **`holderName`**           | <code>string</code> |
| **`extendedHolderName`**   | <code>string</code> |
| **`installments`**         | <code>string</code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### PaymentEvent

| Prop          | Type                                                          |
| ------------- | ------------------------------------------------------------- |
| **`code`**    | <code><a href="#paymenteventcode">PaymentEventCode</a></code> |
| **`message`** | <code>string</code>                                           |


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
