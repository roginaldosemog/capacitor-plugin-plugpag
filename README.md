# capacitor-plugin-plugpag

Plugin for PagSeguro Maquininha's PlugPag SDK

## Install

```bash
npm install capacitor-plugin-plugpag
npx cap sync
```

## API

<docgen-index>

* [`initialize(...)`](#initialize)
* [`doPayment(...)`](#dopayment)
* [`addListener('paymentProgress', ...)`](#addlistenerpaymentprogress-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### initialize(...)

```typescript
initialize(options: { activationCode: string; }) => Promise<{ status: string; }>
```

Inicializa e ativa o terminal

| Param         | Type                                     |
| ------------- | ---------------------------------------- |
| **`options`** | <code>{ activationCode: string; }</code> |

**Returns:** <code>Promise&lt;{ status: string; }&gt;</code>

--------------------


### doPayment(...)

```typescript
doPayment(options: { type: number; amount: number; installmentType?: number; installments?: number; userReference?: string; printReceipt?: boolean; }) => Promise<any>
```

Realiza um pagamento

| Param         | Type                                                                                                                                            |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ type: number; amount: number; installmentType?: number; installments?: number; userReference?: string; printReceipt?: boolean; }</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('paymentProgress', ...)

```typescript
addListener(eventName: 'paymentProgress', listenerFunc: (info: { message: string; code: number; }) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Escuta eventos da maquininha (mensagens de senha, processamento, etc)

| Param              | Type                                                               |
| ------------------ | ------------------------------------------------------------------ |
| **`eventName`**    | <code>'paymentProgress'</code>                                     |
| **`listenerFunc`** | <code>(info: { message: string; code: number; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Remove todos os listeners

--------------------


### Interfaces


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

</docgen-api>
