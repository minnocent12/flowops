import { useState, useEffect } from 'react'
import './App.css'

function useApi(url, refreshKey) {
  const [state, setState] = useState({ data: null, loading: true, error: null })
  useEffect(() => {
    let cancelled = false
    setState(s => ({ ...s, loading: true, error: null }))
    fetch(url)
      .then(r => { if (!r.ok) throw new Error(`HTTP ${r.status}`); return r.json() })
      .then(data => { if (!cancelled) setState({ data, loading: false, error: null }) })
      .catch(e  => { if (!cancelled) setState({ data: null, loading: false, error: e.message }) })
    return () => { cancelled = true }
  }, [url, refreshKey])
  return state
}

function Skeleton({ width = 60, height = 32 }) {
  return <span className="skeleton" style={{ width, height }} />
}

function MetricCard({ icon, label, value, loading, accent }) {
  return (
    <div className="metric-card" style={{ '--card-accent': accent }}>
      <div className="metric-top">
        <span className="metric-label">{label}</span>
        <span className="metric-icon">{icon}</span>
      </div>
      <div className="metric-value">
        {loading ? <Skeleton /> : (value ?? '—')}
      </div>
    </div>
  )
}

function SectionHeader({ title, count }) {
  return (
    <div className="section-header">
      <h2>{title}</h2>
      {count != null && <span className="row-count">{count}</span>}
    </div>
  )
}

function MapTable({ data, loading }) {
  if (loading) return <span className="muted">Loading…</span>
  if (!data || Object.keys(data).length === 0) return <span className="muted">No data yet</span>
  return (
    <table>
      <tbody>
        {Object.entries(data).map(([k, v]) => (
          <tr key={k}>
            <td className="map-key">{k}</td>
            <td className="num map-val">{v}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function StatusBadge({ status }) {
  const cls = status === 'ACCEPTED' || status === 'CREATED'
    ? 'badge green'
    : status?.startsWith('REJECTED')
    ? 'badge red'
    : 'badge grey'
  return <span className={cls}>{status}</span>
}

function rowStatusClass(status) {
  if (status === 'ACCEPTED' || status === 'CREATED') return 'row-status-green'
  if (status?.startsWith('REJECTED'))                return 'row-status-red'
  return ''
}

function OrdersTable({ data, loading, error }) {
  if (loading) return <span className="muted">Loading…</span>
  if (error)   return <span className="error">Failed to load: {error}</span>
  if (!data?.length) return <span className="muted">No orders yet</span>
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Customer</th>
            <th>SKU</th>
            <th>Qty</th>
            <th>Status</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          {data.map((o, i) => (
            <tr key={o.id} className={`${i % 2 !== 0 ? 'row-alt' : ''} ${rowStatusClass(o.status)}`}>
              <td className="mono">{o.id?.slice(0, 8)}…</td>
              <td className="fw-medium">{o.customerId}</td>
              <td><span className="chip">{o.sku}</span></td>
              <td className="num">{o.quantity}</td>
              <td><StatusBadge status={o.status} /></td>
              <td className="text-muted">{o.createdAt?.slice(0, 19).replace('T', ' ')}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function ShipmentsTable({ data, loading, error }) {
  if (loading) return <span className="muted">Loading…</span>
  if (error)   return <span className="error">Failed to load: {error}</span>
  if (!data?.length) return <span className="muted">No shipments yet</span>
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Order ID</th>
            <th>Warehouse</th>
            <th>Carrier</th>
            <th>Tracking ID</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {data.map((s, i) => (
            <tr key={s.id} className={`${i % 2 !== 0 ? 'row-alt' : ''} ${rowStatusClass(s.status)}`}>
              <td className="mono">{s.orderId?.slice(0, 8)}…</td>
              <td className="fw-medium">{s.warehouseId}</td>
              <td><span className="carrier-tag">{s.carrier}</span></td>
              <td className="mono cyan">{s.trackingId}</td>
              <td><StatusBadge status={s.status} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function InventoryTable({ data, loading, error }) {
  if (loading) return <span className="muted">Loading…</span>
  if (error)   return <span className="error">Failed to load: {error}</span>
  if (!data?.length) return <span className="muted">No inventory data</span>

  const max = Math.max(...data.map(i => i.quantity), 1)

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Warehouse</th>
            <th>SKU</th>
            <th style={{ width: '38%' }}>Stock Level</th>
            <th>Qty</th>
          </tr>
        </thead>
        <tbody>
          {data.map((i, idx) => (
            <tr key={i.id} className={idx % 2 !== 0 ? 'row-alt' : ''}>
              <td className="fw-medium">{i.warehouseId}</td>
              <td><span className="chip">{i.sku}</span></td>
              <td>
                <div className="stock-bar-wrap">
                  <div className="stock-bar" style={{ width: `${(i.quantity / max) * 100}%` }} />
                </div>
              </td>
              <td className="num fw-medium">{i.quantity.toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function ActivityFeed({ orders, shipments, loading }) {
  if (loading) return <span className="muted">Loading…</span>

  const events = []

  ;(orders || []).forEach(o => {
    const isRejected = o.status?.startsWith('REJECTED')
    events.push({
      key:    o.id + '-order',
      type:   isRejected ? 'rejected' : 'accepted',
      time:   o.createdAt,
      title:  isRejected ? 'Order rejected' : 'Order accepted',
      detail: `${o.customerId} · ${o.sku} ×${o.quantity}`,
    })
  })

  ;(shipments || []).forEach(s => {
    events.push({
      key:    s.id + '-ship',
      type:   'shipment',
      time:   s.createdAt,
      title:  'Shipment created',
      detail: `${s.trackingId} · ${s.carrier} · ${s.warehouseId}`,
    })
  })

  events.sort((a, b) => new Date(b.time) - new Date(a.time))

  if (!events.length) return <span className="muted">No activity yet</span>

  return (
    <div className="feed">
      {events.map(e => (
        <div key={e.key} className="feed-item">
          <span className={`feed-dot dot-${e.type}`} />
          <div className="feed-body">
            <div className="feed-title">{e.title}</div>
            <div className="feed-detail">{e.detail}</div>
          </div>
          <div className="feed-time">{e.time?.slice(11, 19)}</div>
        </div>
      ))}
    </div>
  )
}

export default function App() {
  const [refreshKey, setRefreshKey]   = useState(0)
  const [lastUpdated, setLastUpdated] = useState(new Date())

  const refresh = () => {
    setRefreshKey(k => k + 1)
    setLastUpdated(new Date())
  }

  const orderMetrics = useApi('/proxy/orders/api/metrics',      refreshKey)
  const shipMetrics  = useApi('/proxy/shipments/api/metrics',   refreshKey)
  const orders       = useApi('/proxy/orders/api/orders',       refreshKey)
  const shipments    = useApi('/proxy/shipments/api/shipments', refreshKey)
  const inventory    = useApi('/proxy/inventory/api/inventory', refreshKey)

  const timeStr = lastUpdated.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })

  return (
    <div className="app">
      <header className="top-bar">
        <div className="top-bar-left">
          <div className="logo">
            <span className="logo-mark">F</span>
            <span className="logo-text">FlowOps</span>
          </div>
          <span className="divider" />
          <span className="subtitle">Order Fulfillment</span>
        </div>
        <div className="top-bar-right">
          <span className="last-updated">
            <span className="live-dot" />
            Updated {timeStr}
          </span>
          <button className="refresh-btn" onClick={refresh}>
            <span className="refresh-icon">↻</span> Refresh
          </button>
        </div>
      </header>

      <main>
        <section className="metrics-row">
          <MetricCard icon="◈" label="Total Orders"    accent="#6366f1" value={orderMetrics.data?.totalOrders}    loading={orderMetrics.loading} />
          <MetricCard icon="✓" label="Accepted"        accent="#22d3ee" value={orderMetrics.data?.acceptedOrders} loading={orderMetrics.loading} />
          <MetricCard icon="✕" label="Rejected"        accent="#f87171" value={orderMetrics.data?.rejectedOrders} loading={orderMetrics.loading} />
          <MetricCard icon="%" label="Acceptance Rate" accent="#a78bfa" value={orderMetrics.data != null ? `${orderMetrics.data.acceptanceRate}%` : null} loading={orderMetrics.loading} />
          <MetricCard icon="→" label="Total Shipments" accent="#34d399" value={shipMetrics.data?.totalShipments}  loading={shipMetrics.loading} />
        </section>

        <div className="two-col">
          <section className="card">
            <SectionHeader title="Shipments by Warehouse" />
            <MapTable data={shipMetrics.data?.shipmentsByWarehouse} loading={shipMetrics.loading} />
          </section>
          <section className="card">
            <SectionHeader title="Shipments by Carrier" />
            <MapTable data={shipMetrics.data?.shipmentsByCarrier} loading={shipMetrics.loading} />
          </section>
        </div>

        <div className="orders-feed-row">
          <section className="card">
            <SectionHeader title="Orders" count={orders.data?.length} />
            <OrdersTable data={orders.data} loading={orders.loading} error={orders.error} />
          </section>
          <section className="card">
            <SectionHeader title="Activity Feed" count={
              (orders.data?.length ?? 0) + (shipments.data?.length ?? 0) || null
            } />
            <ActivityFeed
              orders={orders.data}
              shipments={shipments.data}
              loading={orders.loading && shipments.loading}
            />
          </section>
        </div>

        <section className="card">
          <SectionHeader title="Shipments" count={shipments.data?.length} />
          <ShipmentsTable data={shipments.data} loading={shipments.loading} error={shipments.error} />
        </section>

        <section className="card">
          <SectionHeader title="Inventory" count={inventory.data?.length} />
          <InventoryTable data={inventory.data} loading={inventory.loading} error={inventory.error} />
        </section>
      </main>
    </div>
  )
}
