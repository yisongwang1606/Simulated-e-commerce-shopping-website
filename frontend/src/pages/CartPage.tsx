import { type FormEvent, useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { createAddress, getAddresses, setDefaultAddress } from '../api/addresses'
import { getCart, removeCartItem, updateCartItem } from '../api/cart'
import { createOrder } from '../api/orders'
import type { Cart, CustomerAddress, CustomerAddressInput } from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { formatCurrency } from '../shared/formatters'
import { setIndexedValue, setObjectField } from '../shared/state'
import { EmptyState } from '../shared/ui/EmptyState'
import { LoadingState } from '../shared/ui/LoadingState'
import { SectionHeading } from '../shared/ui/SectionHeading'

const initialAddressForm: CustomerAddressInput = {
  addressLabel: 'Office',
  receiverName: '',
  phone: '',
  line1: '',
  line2: '',
  city: '',
  province: 'Alberta',
  postalCode: '',
  isDefault: false,
}

export function CartPage() {
  const navigate = useNavigate()
  const [cart, setCart] = useState<Cart | null>(null)
  const [addresses, setAddresses] = useState<CustomerAddress[]>([])
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null)
  const [addressForm, setAddressForm] =
    useState<CustomerAddressInput>(initialAddressForm)
  const [draftQuantities, setDraftQuantities] = useState<Record<number, number>>({})
  const [showAddressForm, setShowAddressForm] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isSavingAddress, setIsSavingAddress] = useState(false)
  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const syncSelectedAddress = useCallback(
    (nextAddresses: CustomerAddress[], preferredAddressId?: number | null) => {
      setSelectedAddressId((currentSelectedAddressId) => {
        const nextSelectedAddress =
          nextAddresses.find((address) => address.id === preferredAddressId) ??
          nextAddresses.find(
            (address) => address.id === currentSelectedAddressId,
          ) ??
          nextAddresses.find((address) => address.isDefault) ??
          nextAddresses[0]

        return nextSelectedAddress?.id ?? null
      })
    },
    [],
  )

  const loadCheckoutData = useCallback(async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const [cartData, addressData] = await Promise.all([getCart(), getAddresses()])
      setCart(cartData)
      setAddresses(addressData)
      setDraftQuantities(
        Object.fromEntries(
          cartData.items.map((item) => [item.productId, item.quantity]),
        ),
      )
      syncSelectedAddress(addressData)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }, [syncSelectedAddress])

  useEffect(() => {
    void loadCheckoutData()
  }, [loadCheckoutData])

  function updateAddressField<K extends keyof CustomerAddressInput>(
    field: K,
    value: CustomerAddressInput[K],
  ) {
    setAddressForm((current) => setObjectField(current, field, value))
  }

  function updateDraftQuantity(productId: number, quantity: number) {
    setDraftQuantities((current) => setIndexedValue(current, productId, quantity))
  }

  async function handleUpdate(productId: number) {
    setIsSubmitting(true)
    setMessage('')
    setErrorMessage('')

    try {
      const responseMessage = await updateCartItem(productId, {
        quantity: draftQuantities[productId],
      })
      setMessage(responseMessage)
      await loadCheckoutData()
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleRemove(productId: number) {
    setIsSubmitting(true)
    setMessage('')
    setErrorMessage('')

    try {
      const responseMessage = await removeCartItem(productId)
      setMessage(responseMessage)
      await loadCheckoutData()
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleMakeDefault(addressId: number) {
    setIsSavingAddress(true)
    setMessage('')
    setErrorMessage('')

    try {
      const updatedAddress = await setDefaultAddress(addressId)
      const addressData = await getAddresses()
      setAddresses(addressData)
      syncSelectedAddress(addressData, updatedAddress.id)
      setMessage(`Default shipping address set to ${updatedAddress.addressLabel}.`)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSavingAddress(false)
    }
  }

  async function handleCreateAddress(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSavingAddress(true)
    setMessage('')
    setErrorMessage('')

    try {
      const createdAddress = await createAddress(addressForm)
      const addressData = await getAddresses()
      setAddresses(addressData)
      syncSelectedAddress(addressData, createdAddress.id)
      setAddressForm(initialAddressForm)
      setShowAddressForm(false)
      setMessage(`Address "${createdAddress.addressLabel}" added to checkout.`)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSavingAddress(false)
    }
  }

  async function handleCheckout() {
    if (!selectedAddressId) {
      setErrorMessage('Add and select a shipping address before placing the order.')
      return
    }

    setIsSubmitting(true)
    setMessage('')
    setErrorMessage('')

    try {
      const order = await createOrder({ addressId: selectedAddressId })
      setMessage(`Order ${order.orderNo} created successfully.`)
      await loadCheckoutData()
      navigate('/orders')
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  const selectedAddress =
    addresses.find((address) => address.id === selectedAddressId) ?? null
  const shouldShowAddressForm = showAddressForm || addresses.length === 0

  return (
    <div className="stack-lg">
      <section className="surface stack-lg">
        <SectionHeading
          description="Redis still drives the live cart, but checkout now also captures a customer-selected shipping address for the order snapshot."
          eyebrow="Cart"
          title="Review the cart and lock the delivery address"
        />

        {message ? <div className="message">{message}</div> : null}
        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        {isLoading ? (
          <LoadingState title="Loading the Redis cart..." />
        ) : cart && cart.items.length > 0 ? (
          <div className="cart-layout">
            <div className="stack-lg">
              {cart.items.map((item) => (
                <article className="cart-item" key={item.productId}>
                  <div className="cart-item-main">
                    <div className="cart-copy">
                      <h3 className="card-title">{item.name}</h3>
                      <div className="item-meta">
                        <span className="pill">{item.category}</span>
                        <span>{formatCurrency(item.price)} each</span>
                      </div>
                    </div>

                    <div className="cart-side">
                      <div className="field cart-qty-field">
                        <label htmlFor={`qty-${item.productId}`}>Qty</label>
                        <input
                          id={`qty-${item.productId}`}
                          min={1}
                          onChange={(event) =>
                            updateDraftQuantity(item.productId, Number(event.target.value))
                          }
                          type="number"
                          value={draftQuantities[item.productId] ?? item.quantity}
                        />
                      </div>
                      <div className="cart-price-block">
                        <span className="eyebrow">Subtotal</span>
                        <strong className="price">{formatCurrency(item.subtotal)}</strong>
                      </div>
                      <div className="stack-row">
                        <button
                          className="button-outline"
                          disabled={isSubmitting || isSavingAddress}
                          onClick={() => void handleUpdate(item.productId)}
                          type="button"
                        >
                          Update
                        </button>
                        <button
                          className="button-ghost"
                          disabled={isSubmitting || isSavingAddress}
                          onClick={() => void handleRemove(item.productId)}
                          type="button"
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  </div>
                </article>
              ))}
            </div>

            <aside className="detail-panel cart-summary cart-summary-sticky">
              <div className="summary-section">
                <p className="eyebrow">Shipping destination</p>

                {addresses.length > 0 ? (
                  <div className="address-list">
                    {addresses.map((address) => (
                      <article
                        className={`address-card${
                          selectedAddressId === address.id ? ' active' : ''
                        }`}
                        key={address.id}
                      >
                        <div className="address-card-top">
                          <label className="address-selector" htmlFor={`address-${address.id}`}>
                            <input
                              checked={selectedAddressId === address.id}
                              id={`address-${address.id}`}
                              name="checkout-address"
                              onChange={() => setSelectedAddressId(address.id)}
                              type="radio"
                            />
                            <span className="stack">
                              <strong>{address.addressLabel}</strong>
                              <span className="supporting-copy">
                                {address.receiverName} · {address.phone}
                              </span>
                            </span>
                          </label>
                          {address.isDefault ? <span className="pill">Default</span> : null}
                        </div>

                        <p className="supporting-copy">
                          {address.line1}
                          {address.line2 ? `, ${address.line2}` : ''}
                          <br />
                          {address.city}, {address.province} {address.postalCode}
                        </p>

                        {!address.isDefault ? (
                          <button
                            className="button-ghost"
                            disabled={isSavingAddress || isSubmitting}
                            onClick={() => void handleMakeDefault(address.id)}
                            type="button"
                          >
                            Make default
                          </button>
                        ) : null}
                      </article>
                    ))}
                  </div>
                ) : (
                  <div className="address-empty-block">
                    <p className="supporting-copy">
                      Add a shipping address before you place the order.
                    </p>
                  </div>
                )}

                <button
                  className="button-outline"
                  disabled={isSavingAddress || isSubmitting}
                  onClick={() => setShowAddressForm((current) => !current)}
                  type="button"
                >
                  {shouldShowAddressForm ? 'Hide address form' : 'Add another address'}
                </button>

                {shouldShowAddressForm ? (
                  <form
                    className="form-grid compact address-form"
                    onSubmit={(event) => void handleCreateAddress(event)}
                  >
                    <div className="form-columns">
                      <div className="field">
                        <label htmlFor="address-label">Label</label>
                        <input
                          id="address-label"
                          onChange={(event) =>
                            updateAddressField('addressLabel', event.target.value)
                          }
                          required
                          value={addressForm.addressLabel}
                        />
                      </div>
                      <div className="field">
                        <label htmlFor="receiver-name">Receiver</label>
                        <input
                          id="receiver-name"
                          onChange={(event) =>
                            updateAddressField('receiverName', event.target.value)
                          }
                          required
                          value={addressForm.receiverName}
                        />
                      </div>
                      <div className="field">
                        <label htmlFor="receiver-phone">Phone</label>
                        <input
                          id="receiver-phone"
                          onChange={(event) =>
                            updateAddressField('phone', event.target.value)
                          }
                          required
                          value={addressForm.phone}
                        />
                      </div>
                      <div className="field">
                        <label htmlFor="address-city">City</label>
                        <input
                          id="address-city"
                          onChange={(event) =>
                            updateAddressField('city', event.target.value)
                          }
                          required
                          value={addressForm.city}
                        />
                      </div>
                    </div>

                    <div className="field">
                      <label htmlFor="address-line1">Address line 1</label>
                      <input
                        id="address-line1"
                        onChange={(event) => updateAddressField('line1', event.target.value)}
                        required
                        value={addressForm.line1}
                      />
                    </div>

                    <div className="field">
                      <label htmlFor="address-line2">Address line 2</label>
                      <input
                        id="address-line2"
                        onChange={(event) => updateAddressField('line2', event.target.value)}
                        value={addressForm.line2}
                      />
                    </div>

                    <div className="form-columns">
                      <div className="field">
                        <label htmlFor="address-province">Province</label>
                        <input
                          id="address-province"
                          onChange={(event) =>
                            updateAddressField('province', event.target.value)
                          }
                          required
                          value={addressForm.province}
                        />
                      </div>
                      <div className="field">
                        <label htmlFor="address-postal-code">Postal code</label>
                        <input
                          id="address-postal-code"
                          onChange={(event) =>
                            updateAddressField('postalCode', event.target.value)
                          }
                          required
                          value={addressForm.postalCode}
                        />
                      </div>
                    </div>

                    <label className="checkbox-row" htmlFor="address-default">
                      <input
                        checked={addressForm.isDefault}
                        id="address-default"
                        onChange={(event) =>
                          updateAddressField('isDefault', event.target.checked)
                        }
                        type="checkbox"
                      />
                      <span>Make this the default shipping address</span>
                    </label>

                    <button
                      className="button"
                      disabled={isSavingAddress || isSubmitting}
                      type="submit"
                    >
                      {isSavingAddress ? 'Saving address...' : 'Save address'}
                    </button>
                  </form>
                ) : null}
              </div>

              <div className="summary-divider" />

              <div className="summary-section">
                <p className="eyebrow">Order summary</p>
                <div className="summary-row">
                  <span>Total items</span>
                  <strong>{cart.totalQuantity}</strong>
                </div>
                <div className="summary-row">
                  <span>Merchandise total</span>
                  <strong className="price">{formatCurrency(cart.totalPrice)}</strong>
                </div>

                {selectedAddress ? (
                  <div className="summary-address">
                    <span className="signal">Selected address</span>
                    <div className="stack">
                      <strong>{selectedAddress.receiverName}</strong>
                      <span className="supporting-copy">
                        {selectedAddress.line1}
                        {selectedAddress.line2 ? `, ${selectedAddress.line2}` : ''}
                      </span>
                      <span className="supporting-copy">
                        {selectedAddress.city}, {selectedAddress.province}{' '}
                        {selectedAddress.postalCode}
                      </span>
                    </div>
                  </div>
                ) : null}

                <button
                  className="button"
                  disabled={!selectedAddressId || isSubmitting || isSavingAddress}
                  onClick={() => void handleCheckout()}
                  type="button"
                >
                  {isSubmitting ? 'Placing order...' : 'Create order'}
                </button>
                <Link className="button-outline" to="/catalog">
                  Back to catalog
                </Link>
                <p className="supporting-copy">
                  Checkout now writes the selected address into the order snapshot
                  before the Redis cart is cleared.
                </p>
              </div>
            </aside>
          </div>
        ) : (
          <EmptyState
            message="Add a product from the catalog first. This page is ready for real checkout once the cart has items."
            title="The cart is empty"
          />
        )}
      </section>
    </div>
  )
}
