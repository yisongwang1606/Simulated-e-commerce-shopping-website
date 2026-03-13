import { useEffect, useState, type FormEvent } from 'react'
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js'
import { loadStripe } from '@stripe/stripe-js'

import {
  createOrderStripePaymentIntent,
  reconcileOrderStripePayment,
} from '../../api/orders'
import type { Order, PaymentTransaction } from '../../api/contracts'
import { extractErrorMessage } from '../error'
import { formatCurrency, formatDateTime } from '../formatters'
import { LoadingState } from './LoadingState'
import { StatusPill } from './StatusPill'

const stripePublishableKey =
  import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY?.trim() ?? ''
const stripePromise = stripePublishableKey ? loadStripe(stripePublishableKey) : null
const payableStatuses = new Set(['CREATED', 'PAYMENT_PENDING'])

interface StripeOrderPaymentPanelProps {
  order: Order
  payments: PaymentTransaction[]
  onMessage: (message: string) => void
  onError: (message: string) => void
  onPaymentUpdated: () => Promise<void>
}

interface StripeCheckoutFormProps {
  orderId: number
  providerReference: string
  onComplete: (payment: PaymentTransaction) => Promise<void>
  onError: (message: string) => void
  onMessage: (message: string) => void
}

function StripeCheckoutForm({
  orderId,
  providerReference,
  onComplete,
  onError,
  onMessage,
}: StripeCheckoutFormProps) {
  const stripe = useStripe()
  const elements = useElements()
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!stripe || !elements) {
      onError('Stripe.js is still loading. Try again in a moment.')
      return
    }

    setIsSubmitting(true)
    onMessage('')
    onError('')

    try {
      const result = await stripe.confirmPayment({
        elements,
        redirect: 'if_required',
      })

      if (result.error) {
        onError(result.error.message ?? 'Stripe could not confirm the payment.')
        return
      }

      const payment = await reconcileOrderStripePayment(orderId, {
        providerReference: result.paymentIntent?.id ?? providerReference,
      })

      if (payment.paymentStatus === 'SUCCEEDED') {
        onMessage(`Payment ${payment.transactionRef} captured successfully.`)
      } else {
        onMessage(
          `Payment ${payment.transactionRef} is currently ${payment.paymentStatus.toLowerCase()}.`,
        )
      }

      await onComplete(payment)
    } catch (error) {
      onError(extractErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="stack stripe-payment-form" onSubmit={(event) => void handleSubmit(event)}>
      <div className="stripe-element-shell">
        <PaymentElement options={{ layout: 'tabs' }} />
      </div>
      <div className="stripe-pay-actions">
        <button
          className="button"
          disabled={!stripe || !elements || isSubmitting}
          type="submit"
        >
          {isSubmitting ? 'Confirming payment...' : 'Pay securely now'}
        </button>
        <span className="supporting-copy">
          Stripe test mode is enabled. Card details stay inside Stripe Elements.
        </span>
      </div>
    </form>
  )
}

export function StripeOrderPaymentPanel({
  order,
  payments,
  onMessage,
  onError,
  onPaymentUpdated,
}: StripeOrderPaymentPanelProps) {
  const [checkoutPayment, setCheckoutPayment] = useState<PaymentTransaction | null>(null)
  const [isPreparing, setIsPreparing] = useState(false)

  const latestPayment = payments[0] ?? null
  const successfulPayment =
    payments.find((payment) => payment.paymentStatus === 'SUCCEEDED') ?? null
  const isPayable = payableStatuses.has(order.status) && successfulPayment === null

  useEffect(() => {
    if (successfulPayment) {
      setCheckoutPayment(null)
    }
  }, [successfulPayment])

  async function handlePrepareCheckout() {
    setIsPreparing(true)
    onMessage('')
    onError('')

    try {
      const payment = await createOrderStripePaymentIntent(order.id, {
        note: 'Customer checkout via Stripe Elements',
      })

      if (!payment.clientSecret || !payment.providerReference) {
        onError('The payment session was created without a Stripe client secret.')
        return
      }

      setCheckoutPayment(payment)
      onMessage(`Payment ${payment.transactionRef} is ready for card entry.`)
      await onPaymentUpdated()
    } catch (error) {
      onError(extractErrorMessage(error))
    } finally {
      setIsPreparing(false)
    }
  }

  async function handlePaymentCompleted(payment: PaymentTransaction) {
    setCheckoutPayment(
      payment.paymentStatus === 'SUCCEEDED'
        ? null
        : {
            ...payment,
            clientSecret: checkoutPayment?.clientSecret ?? payment.clientSecret,
          },
    )
    await onPaymentUpdated()
  }

  return (
    <section className="info-card stack stripe-payment-panel">
      <div className="toolbar">
        <div>
          <p className="eyebrow">Payments</p>
          <h4 className="card-title">Stripe checkout and payment ledger</h4>
        </div>
        <span className="signal">{payments.length} transactions</span>
      </div>

      <div className="payment-panel-summary">
        <div className="info-card">
          <p className="eyebrow">Amount due</p>
          <strong>{formatCurrency(order.totalPrice)}</strong>
        </div>
        <div className="info-card">
          <p className="eyebrow">Current state</p>
          <div className="stack">
            <StatusPill value={successfulPayment?.paymentStatus ?? order.status} />
            <span className="supporting-copy">
              {successfulPayment
                ? `Paid on ${formatDateTime(successfulPayment.paidAt ?? successfulPayment.updatedAt)}`
                : 'Awaiting successful settlement'}
            </span>
          </div>
        </div>
      </div>

      {payments.length > 0 ? (
        <div className="stack payment-history">
          {payments.map((payment) => (
            <div className="detail-row payment-row" key={payment.id}>
              <div className="stack">
                <strong>{payment.transactionRef}</strong>
                <span className="supporting-copy">
                  {payment.providerCode} {payment.providerReference ? `| ${payment.providerReference}` : ''}
                </span>
                <span className="supporting-copy">
                  {formatCurrency(payment.amount)} | {formatDateTime(payment.createdAt)}
                </span>
                {payment.note ? (
                  <span className="supporting-copy">{payment.note}</span>
                ) : null}
              </div>
              <StatusPill value={payment.paymentStatus} />
            </div>
          ))}
        </div>
      ) : (
        <p className="supporting-copy">
          No payment attempts have been recorded for this order yet.
        </p>
      )}

      {!stripePromise ? (
        <div className="address-empty-block">
          <strong>Stripe checkout is not configured.</strong>
          <p className="supporting-copy">
            Add `VITE_STRIPE_PUBLISHABLE_KEY` to the frontend environment before opening the
            embedded card form.
          </p>
        </div>
      ) : null}

      {isPayable && stripePromise ? (
        <div className="stack refund-form">
          <div className="toolbar">
            <div>
              <p className="eyebrow">Customer checkout</p>
              <h4 className="card-title">Pay this order with Stripe Elements</h4>
            </div>
            {latestPayment ? <StatusPill value={latestPayment.paymentStatus} /> : null}
          </div>

          {checkoutPayment?.clientSecret && checkoutPayment.providerReference ? (
            <Elements
              options={{
                clientSecret: checkoutPayment.clientSecret,
                appearance: {
                  theme: 'stripe',
                  variables: {
                    colorPrimary: '#1f6a59',
                    borderRadius: '12px',
                  },
                },
              }}
              stripe={stripePromise}
            >
              <StripeCheckoutForm
                onComplete={handlePaymentCompleted}
                onError={onError}
                onMessage={onMessage}
                orderId={order.id}
                providerReference={checkoutPayment.providerReference}
              />
            </Elements>
          ) : isPreparing ? (
            <LoadingState title="Preparing secure Stripe checkout..." />
          ) : (
            <button className="button" onClick={() => void handlePrepareCheckout()} type="button">
              Open card payment form
            </button>
          )}
        </div>
      ) : null}
    </section>
  )
}
