"use client"

import { useEffect, useRef, useState } from "react"
import QRCode from "qrcode"
import { Link2, Wifi, Type, Download, Check } from "lucide-react"

type CreateType = "text" | "url" | "wifi"

const tabs: { id: CreateType; label: string; icon: typeof Type; placeholder: string }[] = [
  { id: "text", label: "Văn bản", icon: Type, placeholder: "Nhập nội dung văn bản…" },
  { id: "url", label: "Liên kết", icon: Link2, placeholder: "https://vi-du.com" },
  { id: "wifi", label: "Wi-Fi", icon: Wifi, placeholder: "Tên mạng (SSID)" },
]

export function CreateScreen() {
  const [type, setType] = useState<CreateType>("url")
  const [value, setValue] = useState("")
  const [wifiPass, setWifiPass] = useState("")
  const [copied, setCopied] = useState(false)
  const canvasRef = useRef<HTMLCanvasElement>(null)

  const payload =
    type === "wifi"
      ? value
        ? `WIFI:T:WPA;S:${value};P:${wifiPass};;`
        : ""
      : value

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    if (!payload) {
      const ctx = canvas.getContext("2d")
      ctx?.clearRect(0, 0, canvas.width, canvas.height)
      return
    }
    QRCode.toCanvas(canvas, payload, {
      width: 220,
      margin: 1,
      color: { dark: "#0b1220", light: "#ffffff" },
      errorCorrectionLevel: "M",
    }).catch(() => {})
  }, [payload])

  const activeTab = tabs.find((t) => t.id === type)!

  function handleDownload() {
    const canvas = canvasRef.current
    if (!canvas || !payload) return
    const link = document.createElement("a")
    link.download = "qr-code.png"
    link.href = canvas.toDataURL("image/png")
    link.click()
    setCopied(true)
    setTimeout(() => setCopied(false), 1800)
  }

  return (
    <div className="flex flex-col gap-6 py-4">
      {/* Type tabs */}
      <div className="flex gap-2 rounded-2xl border border-border bg-card p-1.5">
        {tabs.map((tab) => {
          const Icon = tab.icon
          const active = tab.id === type
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => {
                setType(tab.id)
                setValue("")
                setWifiPass("")
              }}
              className={`flex flex-1 items-center justify-center gap-1.5 rounded-xl px-3 py-2.5 text-xs font-medium transition-colors ${
                active
                  ? "bg-scan text-scan-foreground"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              <Icon className="h-4 w-4" />
              {tab.label}
            </button>
          )
        })}
      </div>

      {/* QR preview */}
      <div className="flex flex-col items-center">
        <div className="relative rounded-3xl border border-border bg-card p-6 shadow-2xl">
          <div
            aria-hidden="true"
            className="absolute -inset-4 -z-10 rounded-[2rem] bg-scan/10 blur-2xl"
          />
          <div className="flex h-[220px] w-[220px] items-center justify-center overflow-hidden rounded-2xl bg-white">
            {payload ? (
              <canvas ref={canvasRef} className="h-full w-full" />
            ) : (
              <p className="px-6 text-center text-xs text-neutral-400">
                Nhập nội dung để tạo mã QR
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Inputs */}
      <div className="flex flex-col gap-3">
        <div>
          <label className="mb-1.5 block text-xs font-medium text-muted-foreground">
            {activeTab.label}
          </label>
          <input
            value={value}
            onChange={(e) => setValue(e.target.value)}
            placeholder={activeTab.placeholder}
            className="w-full rounded-2xl border border-border bg-card px-4 py-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-scan/60"
          />
        </div>
        {type === "wifi" && (
          <div>
            <label className="mb-1.5 block text-xs font-medium text-muted-foreground">
              Mật khẩu
            </label>
            <input
              value={wifiPass}
              onChange={(e) => setWifiPass(e.target.value)}
              placeholder="Mật khẩu Wi-Fi"
              className="w-full rounded-2xl border border-border bg-card px-4 py-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-scan/60"
            />
          </div>
        )}
      </div>

      {/* Download */}
      <button
        type="button"
        onClick={handleDownload}
        disabled={!payload}
        className="flex items-center justify-center gap-2 rounded-2xl bg-scan px-5 py-3.5 text-sm font-semibold text-scan-foreground transition-opacity disabled:cursor-not-allowed disabled:opacity-40"
      >
        {copied ? <Check className="h-4 w-4" /> : <Download className="h-4 w-4" />}
        {copied ? "Đã lưu mã QR" : "Tải mã QR"}
      </button>
    </div>
  )
}
