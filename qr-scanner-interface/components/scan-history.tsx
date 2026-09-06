"use client"

import { ChevronRight } from "lucide-react"
import { typeMeta, type ScanItem } from "@/lib/qr-history"

function HistoryCard({ item }: { item: ScanItem }) {
  const { icon: Icon, label } = typeMeta[item.type]
  return (
    <button
      type="button"
      className="group flex w-full items-center gap-3 rounded-2xl border border-border bg-card p-3 text-left transition-colors hover:border-scan/40 hover:bg-secondary/60"
    >
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-secondary text-scan">
        <Icon className="h-5 w-5" />
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-foreground">{item.title}</p>
        <p className="truncate text-xs text-muted-foreground">
          {label} · {item.subtitle}
        </p>
      </div>
      <div className="flex shrink-0 items-center gap-1">
        <span className="text-xs text-muted-foreground">{item.time}</span>
        <ChevronRight className="h-4 w-4 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
      </div>
    </button>
  )
}

export function ScanHistory({
  items,
  onViewAll,
}: {
  items: ScanItem[]
  onViewAll: () => void
}) {
  const preview = items.slice(0, 4)
  return (
    <section className="w-full" aria-label="Lịch sử quét">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-semibold text-foreground">Lịch sử quét</h2>
        <button
          type="button"
          onClick={onViewAll}
          className="text-xs font-medium text-scan hover:underline"
        >
          Xem tất cả
        </button>
      </div>
      {preview.length === 0 ? (
        <p className="rounded-2xl border border-border bg-card p-4 text-center text-xs text-muted-foreground">
          Chưa có mã nào được quét.
        </p>
      ) : (
        <ul className="flex flex-col gap-2.5">
          {preview.map((item) => (
            <li key={item.id}>
              <HistoryCard item={item} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
