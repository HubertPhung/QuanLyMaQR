import { Zap } from "lucide-react"

function Corner({ className }: { className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={`absolute h-8 w-8 border-scan/90 ${className}`}
    />
  )
}

export function QrScannerFrame() {
  return (
    <div className="relative mx-auto aspect-square w-full max-w-[300px]">
      {/* soft glow behind the frame */}
      <div
        aria-hidden="true"
        className="absolute -inset-6 rounded-[2.5rem] bg-scan/10 blur-2xl"
      />

      {/* camera viewport */}
      <div className="relative h-full w-full overflow-hidden rounded-[2rem] border border-border bg-card shadow-2xl">
        {/* faux camera texture */}
        <div
          aria-hidden="true"
          className="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,oklch(0.3_0.04_255)_0%,transparent_55%),radial-gradient(circle_at_75%_80%,oklch(0.28_0.05_200)_0%,transparent_50%)]"
        />
        {/* grid overlay */}
        <div
          aria-hidden="true"
          className="absolute inset-0 opacity-[0.15] [background-image:linear-gradient(oklch(1_0_0)_1px,transparent_1px),linear-gradient(90deg,oklch(1_0_0)_1px,transparent_1px)] [background-size:28px_28px]"
        />

        {/* mock QR target in the middle */}
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="grid h-24 w-24 grid-cols-4 grid-rows-4 gap-1 opacity-25">
            {Array.from({ length: 16 }).map((_, i) => (
              <span
                key={i}
                className={`rounded-[3px] ${i % 3 === 0 || i % 5 === 0 ? "bg-foreground" : "bg-transparent"}`}
              />
            ))}
          </div>
        </div>

        {/* scanning line */}
        <div className="absolute inset-x-0 top-0 h-full">
          <div className="animate-scan-line absolute inset-x-4 h-px bg-scan shadow-[0_0_12px_2px_var(--scan)]">
            <div className="absolute inset-x-0 -top-16 h-16 bg-gradient-to-b from-transparent to-scan/25" />
          </div>
        </div>

        {/* corner brackets */}
        <Corner className="left-4 top-4 rounded-tl-xl border-l-2 border-t-2" />
        <Corner className="right-4 top-4 rounded-tr-xl border-r-2 border-t-2" />
        <Corner className="bottom-4 left-4 rounded-bl-xl border-b-2 border-l-2" />
        <Corner className="bottom-4 right-4 rounded-br-xl border-b-2 border-r-2" />
      </div>

      {/* status pill */}
      <div className="animate-scan-pulse absolute -bottom-4 left-1/2 flex -translate-x-1/2 items-center gap-2 rounded-full border border-scan/30 bg-card px-4 py-2 shadow-lg">
        <Zap className="h-4 w-4 text-scan" fill="currentColor" />
        <span className="text-sm font-medium text-foreground">Đang quét…</span>
      </div>
    </div>
  )
}
