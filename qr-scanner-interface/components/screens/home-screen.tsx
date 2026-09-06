"use client"

import { ScanLine, QrCode, ChevronRight, TrendingUp } from "lucide-react"
import { typeMeta, type ScanItem } from "@/lib/qr-history"

export function HomeScreen({
  history,
  onScan,
  onCreate,
  onViewAll,
}: {
  history: ScanItem[]
  onScan: () => void
  onCreate: () => void
  onViewAll: () => void
}) {
  const recent = history.slice(0, 3)

  return (
    <div className="flex flex-col gap-6 py-4">
      {/* Greeting */}
      <div>
        <p className="text-sm text-muted-foreground">Chào buổi sáng,</p>
        <h2 className="text-2xl font-semibold text-foreground">Minh Anh</h2>
      </div>

      {/* Stat highlight */}
      <div className="relative overflow-hidden rounded-3xl border border-scan/20 bg-gradient-to-br from-card to-secondary/40 p-5">
        <div
          aria-hidden="true"
          className="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-scan/15 blur-2xl"
        />
        <div className="relative flex items-center justify-between">
          <div>
            <div className="flex items-center gap-1.5 text-xs text-scan">
              <TrendingUp className="h-3.5 w-3.5" />
              Tuần này
            </div>
            <p className="mt-2 text-3xl font-semibold text-foreground">{history.length}</p>
            <p className="text-xs text-muted-foreground">mã đã quét</p>
          </div>
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-scan text-scan-foreground">
            <QrCode className="h-7 w-7" />
          </div>
        </div>
      </div>

      {/* Quick actions */}
      <div className="grid grid-cols-2 gap-3">
        <button
          type="button"
          onClick={onScan}
          className="group flex flex-col gap-3 rounded-2xl border border-border bg-card p-4 text-left transition-colors hover:border-scan/40 hover:bg-secondary/60"
        >
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-scan text-scan-foreground">
            <ScanLine className="h-5 w-5" />
          </div>
          <div>
            <p className="text-sm font-semibold text-foreground">Quét mã QR</p>
            <p className="text-xs text-muted-foreground">Mở camera quét ngay</p>
          </div>
        </button>
        <button
          type="button"
          onClick={onCreate}
          className="group flex flex-col gap-3 rounded-2xl border border-border bg-card p-4 text-left transition-colors hover:border-scan/40 hover:bg-secondary/60"
        >
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-secondary text-scan">
            <QrCode className="h-5 w-5" />
          </div>
          <div>
            <p className="text-sm font-semibold text-foreground">Tạo mã QR</p>
            <p className="text-xs text-muted-foreground">Tạo mã của riêng bạn</p>
          </div>
        </button>
      </div>

      {/* Recent */}
      <section aria-label="Quét gần đây">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-foreground">Quét gần đây</h3>
          <button
            type="button"
            onClick={onViewAll}
            className="text-xs font-medium text-scan hover:underline"
          >
            Xem tất cả
          </button>
        </div>
        <ul className="flex flex-col gap-2.5">
          {recent.map((item) => {
            const { icon: Icon, label } = typeMeta[item.type]
            return (
              <li key={item.id}>
                <button
                  type="button"
                  className="group flex w-full items-center gap-3 rounded-2xl border border-border bg-card p-3 text-left transition-colors hover:border-scan/40 hover:bg-secondary/60"
                >
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-secondary text-scan">
                    <Icon className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{item.title}</p>
                    <p className="truncate text-xs text-muted-foreground">{label}</p>
                  </div>
                  <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
                </button>
              </li>
            )
          })}
        </ul>
      </section>
    </div>
  )
}
