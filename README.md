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
* [`addListener('paymentProgress', ...)`](#addlistenerpaymentprogress-)
* [`addListener('voidProgress', ...)`](#addlistenervoidprogress-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)

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
doPayment(options: { type: number; amount: number; installmentType?: number; installments?: number; userReference: string; printReceipt?: boolean; }) => Promise<{ transactionCode: string; transactionId: string; message: string; }>
```

| Param         | Type                                                                                                                                           |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ type: number; amount: number; installmentType?: number; installments?: number; userReference: string; printReceipt?: boolean; }</code> |

**Returns:** <code>Promise&lt;{ transactionCode: string; transactionId: string; message: string; }&gt;</code>

--------------------


### abort()

```typescript
abort() => Promise<{ result: number; }>
```

**Returns:** <code>Promise&lt;{ result: number; }&gt;</code>

--------------------


### voidPayment(...)

```typescript
voidPayment(options: { transactionCode: string; transactionId: string; printReceipt?: boolean; }) => Promise<{ transactionCode: string; transactionId: string; message: string; }>
```

| Param         | Type                                                                                     |
| ------------- | ---------------------------------------------------------------------------------------- |
| **`options`** | <code>{ transactionCode: string; transactionId: string; printReceipt?: boolean; }</code> |

**Returns:** <code>Promise&lt;{ transactionCode: string; transactionId: string; message: string; }&gt;</code>

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
printFromFile(options: { filePath: string; }) => Promise<{ result: number; }>
```

Imprime a partir de um arquivo no dispositivo.

| Param         | Type                               |
| ------------- | ---------------------------------- |
| **`options`** | <code>{ filePath: string; }</code> |

**Returns:** <code>Promise&lt;{ result: number; }&gt;</code>

--------------------


### reprintCustomerReceipt()

```typescript
reprintCustomerReceipt() => Promise<void>
```

Reimprimir o último comprovante do cliente.

--------------------


### addListener('paymentProgress', ...)

```typescript
addListener(eventName: 'paymentProgress', listenerFunc: (info: { message: string; code: number; }) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Escuta o progresso do pagamento (mensagens como 'Insira o cartão', 'Senha', etc)

| Param              | Type                                                               |
| ------------------ | ------------------------------------------------------------------ |
| **`eventName`**    | <code>'paymentProgress'</code>                                     |
| **`listenerFunc`** | <code>(info: { message: string; code: number; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### addListener('voidProgress', ...)

```typescript
addListener(eventName: 'voidProgress', listenerFunc: (info: { message: string; code: number; }) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Escuta o progresso do estorno

| Param              | Type                                                               |
| ------------------ | ------------------------------------------------------------------ |
| **`eventName`**    | <code>'voidProgress'</code>                                        |
| **`listenerFunc`** | <code>(info: { message: string; code: number; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Remove todos os listeners ativos

--------------------


### Interfaces


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

</docgen-api>
