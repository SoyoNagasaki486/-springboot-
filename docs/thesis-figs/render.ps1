# Renders a diagram described by a JSON primitive list into a PNG.
# Geometry is given in centimetres with the origin at the top-left; the image is
# rasterised at the requested dpi, so Word inserts it at the intended physical
# size and the in-figure point sizes match the thesis body text.
# Note: coordinates are converted to pixels explicitly instead of using a world
# transform, because GDI+ also scales pixel-unit fonts under a transform.
# ASCII-only source: every visible string comes from the JSON.
param(
    [Parameter(Mandatory = $true)][string]$Layout
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$data = Get-Content -Raw -Encoding UTF8 -Path $Layout | ConvertFrom-Json

$dpi = 600
if ($data.dpi) { $dpi = [double]$data.dpi }
$k = $dpi / 2.54                       # pixels per centimetre

$w = [int][Math]::Round($data.widthCm * $k)
$h = [int][Math]::Round($data.heightCm * $k)

$bmp = New-Object System.Drawing.Bitmap $w, $h
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$g.Clear([System.Drawing.Color]::White)

$fam = New-Object System.Drawing.FontFamily 'SimSun'
$famHei = New-Object System.Drawing.FontFamily 'SimHei'
$bBlack = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::Black)
$bWhite = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
$sfCenter = New-Object System.Drawing.StringFormat
$sfCenter.Alignment = [System.Drawing.StringAlignment]::Center
$sfCenter.LineAlignment = [System.Drawing.StringAlignment]::Center

function Px([double]$cm) { return [float]($cm * $k) }

function New-Font([string]$which, [double]$pt, [bool]$bold) {
    $px = $pt / 72.0 * $dpi
    $style = [System.Drawing.FontStyle]::Regular
    if ($bold) { $style = [System.Drawing.FontStyle]::Bold }
    $f = $fam
    if ($which -eq 'hei') { $f = $famHei }
    return New-Object System.Drawing.Font $f, ([float]$px), $style, ([System.Drawing.GraphicsUnit]::Pixel)
}

function Draw-Actor($gfx, [double]$x, [double]$y, [string]$label, [double]$scale) {
    # stick figure; (x, y) is the centre of the head
    $hr = 0.24 * $scale
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Black), (Px (0.035 * $scale))
    $gfx.DrawEllipse($pen, (Px ($x - $hr)), (Px ($y - $hr)), (Px (2 * $hr)), (Px (2 * $hr)))
    $bodyTop = $y + $hr
    $bodyBottom = $bodyTop + 0.62 * $scale
    $gfx.DrawLine($pen, (Px $x), (Px $bodyTop), (Px $x), (Px $bodyBottom))
    $armY = $bodyTop + 0.22 * $scale
    $gfx.DrawLine($pen, (Px ($x - 0.34 * $scale)), (Px $armY), (Px ($x + 0.34 * $scale)), (Px $armY))
    $gfx.DrawLine($pen, (Px $x), (Px $bodyBottom), (Px ($x - 0.26 * $scale)), (Px ($bodyBottom + 0.42 * $scale)))
    $gfx.DrawLine($pen, (Px $x), (Px $bodyBottom), (Px ($x + 0.26 * $scale)), (Px ($bodyBottom + 0.42 * $scale)))
    $font = New-Font 'song' 12 $false
    $size = $gfx.MeasureString($label, $font)
    $gfx.DrawString($label, $font, $bBlack, (Px ($x - 0.34 * $scale - $size.Width / 2 / $k)), (Px ($bodyBottom + 0.5 * $scale)))
    $font.Dispose()
    $pen.Dispose()
}

foreach ($it in $data.items) {
    switch ($it.type) {
        'rect' {
            $lw = 0.035
            if ($it.line) { $lw = [double]$it.line }
            $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Black), (Px $lw)
            if ($it.dashed) { $pen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash }
            $r = New-Object System.Drawing.RectangleF (Px $it.x), (Px $it.y), (Px $it.w), (Px $it.h)
            if ($it.fill -ne 'none') { $g.FillRectangle($bWhite, $r) }
            $g.DrawRectangle($pen, $r.X, $r.Y, $r.Width, $r.Height)
            $pen.Dispose()
        }
        'ellipse' {
            $lw = 0.035
            if ($it.line) { $lw = [double]$it.line }
            $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Black), (Px $lw)
            $p = New-Object System.Drawing.Drawing2D.GraphicsPath
            $p.AddEllipse((Px ($it.cx - $it.w / 2)), (Px ($it.cy - $it.h / 2)), (Px $it.w), (Px $it.h))
            $g.FillPath($bWhite, $p)
            $g.DrawPath($pen, $p)
            $pen.Dispose(); $p.Dispose()
        }
        'diamond' {
            $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Black), (Px 0.035)
            $pts = @(
                (New-Object System.Drawing.PointF (Px $it.cx), (Px ($it.cy - $it.h / 2))),
                (New-Object System.Drawing.PointF (Px ($it.cx + $it.w / 2)), (Px $it.cy)),
                (New-Object System.Drawing.PointF (Px $it.cx), (Px ($it.cy + $it.h / 2))),
                (New-Object System.Drawing.PointF (Px ($it.cx - $it.w / 2)), (Px $it.cy))
            )
            $g.FillPolygon($bWhite, [System.Drawing.PointF[]]$pts)
            $g.DrawPolygon($pen, [System.Drawing.PointF[]]$pts)
            $pen.Dispose()
        }
        'line' {
            $lw = 0.03
            if ($it.line) { $lw = [double]$it.line }
            $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Black), (Px $lw)
            if ($it.dashed) { $pen.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash }
            $g.DrawLine($pen, (Px $it.x1), (Px $it.y1), (Px $it.x2), (Px $it.y2))
            $pen.Dispose()
        }
        'path' {
            $pts = @()
            foreach ($pt in $it.points) { $pts += New-Object System.Drawing.PointF (Px $pt[0]), (Px $pt[1]) }
            $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Black), (Px 0.03)
            if ($it.closed) { $g.DrawPolygon($pen, [System.Drawing.PointF[]]$pts) }
            else { $g.DrawLines($pen, [System.Drawing.PointF[]]$pts) }
            $pen.Dispose()
        }
        'text' {
            $ptSize = 12.0
            if ($it.size) { $ptSize = [double]$it.size }
            $which = 'song'
            if ($it.font) { $which = [string]$it.font }
            $bold = $false
            if ($it.bold) { $bold = $true }
            $font = New-Font $which $ptSize $bold
            $anchor = 'center'
            if ($it.anchor) { $anchor = [string]$it.anchor }
            if ($anchor -eq 'center') {
                $g.DrawString($it.text, $font, $bBlack, (Px $it.x), (Px $it.y), $sfCenter)
            } else {
                $sz = $g.MeasureString($it.text, $font)
                $dx = 0.0
                if ($anchor -eq 'right') { $dx = -$sz.Width }
                if ($anchor -eq 'middle') { $dx = -$sz.Width / 2 }
                $g.DrawString($it.text, $font, $bBlack, (Px $it.x + $dx), (Px ($it.y - $sz.Height / 2 / $k)))
            }
            $font.Dispose()
        }
        'actor' {
            $sc = 1.0
            if ($it.scale) { $sc = [double]$it.scale }
            Draw-Actor $g $it.x $it.y $it.label $sc
        }
        default { }
    }
}

$g.Dispose()
$outPath = Join-Path (Split-Path -Parent $Layout) $data.out
# stamp the resolution so Word inserts the picture at its intended physical size
# (and therefore at the point sizes used above) instead of scaling by 96 dpi
$bmp.SetResolution([float]$dpi, [float]$dpi)
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output ('rendered: ' + $outPath + '  ' + $w + 'x' + $h + ' px @ ' + $dpi + 'dpi  (' + $data.widthCm + 'x' + $data.heightCm + ' cm)')
