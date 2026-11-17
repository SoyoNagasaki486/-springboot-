# Renders the LabGuard system architecture diagram to PNG.
# All display text lives in architecture-diagram.json (UTF-8) so that this
# script stays ASCII-only and is safe for Windows PowerShell 5.1 parsing.
param(
    [string]$DataFile
)

$ErrorActionPreference = 'Stop'
if (-not $DataFile) { $DataFile = Join-Path $PSScriptRoot 'architecture-diagram.json' }

Add-Type -AssemblyName System.Drawing

$data = Get-Content -Raw -Encoding UTF8 -Path $DataFile | ConvertFrom-Json
$layers = @($data.layers)
$n = $layers.Count

# ---------------- geometry (logical units, scaled at the end) ----------------
$scale   = 2.0
$W       = 1740
$padX    = 40
$padTop  = 34
$chipW   = 210
$chipGap = 26
$bandW   = 200
$bandGap = 26
$rowH    = 158
$rowGap  = 52
$titleH  = 62
$subH    = 34
$footH   = 84

$bodyH   = $n * $rowH + ($n - 1) * $rowGap
$bodyTop = $padTop + $titleH + $subH + 26
$H       = $bodyTop + $bodyH + $footH + $padTop
$contentX = $padX + $chipW + $chipGap
$contentW = $W - $padX - $bandW - $bandGap - $contentX
$bandX    = $W - $padX - $bandW

# ---------------- palette ----------------
function Col([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
$cTitle   = Col '#15304A'
$cSub     = Col '#5B7085'
$cItems   = Col '#1F3A52'
$cNote    = Col '#6B7C8C'
$cArrow   = Col '#4B7BAE'
$cFooter  = Col '#6B7C8C'
$cRule    = Col '#D8E4EF'

$chip = @{
    normal   = @{ fill = (Col '#E9F1F9'); line = (Col '#4B7BAE'); text = (Col '#17456F') }
    security = @{ fill = (Col '#FCEFD6'); line = (Col '#D29730'); text = (Col '#8A5A11') }
    storage  = @{ fill = (Col '#E7F4ED'); line = (Col '#3F8F66'); text = (Col '#1F5C40') }
}
$box = @{
    normal   = @{ fill = (Col '#FFFFFF'); line = (Col '#A9C4DC') }
    security = @{ fill = (Col '#FFFBF2'); line = (Col '#E0B463') }
    storage  = @{ fill = (Col '#F6FBF8'); line = (Col '#97C9AF') }
}

# ---------------- canvas ----------------
$bmp = New-Object System.Drawing.Bitmap ([int]($W * $scale)), ([int]($H * $scale))
$g   = [System.Drawing.Graphics]::FromImage($bmp)
$g.ScaleTransform($scale, $scale)
$g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$g.Clear([System.Drawing.Color]::White)

$fam  = New-Object System.Drawing.FontFamily 'Microsoft YaHei'
$PU   = [System.Drawing.GraphicsUnit]::Pixel
$fTitle = New-Object System.Drawing.Font $fam, 30, ([System.Drawing.FontStyle]::Bold), $PU
$fSub   = New-Object System.Drawing.Font $fam, 17, ([System.Drawing.FontStyle]::Regular), $PU
$fChip  = New-Object System.Drawing.Font $fam, 26, ([System.Drawing.FontStyle]::Bold), $PU
$fItems = New-Object System.Drawing.Font $fam, 21, ([System.Drawing.FontStyle]::Regular), $PU
$fNote  = New-Object System.Drawing.Font $fam, 15, ([System.Drawing.FontStyle]::Regular), $PU
$fBandT = New-Object System.Drawing.Font $fam, 17, ([System.Drawing.FontStyle]::Bold), $PU
$fBandL = New-Object System.Drawing.Font $fam, 14, ([System.Drawing.FontStyle]::Regular), $PU
$fFoot  = New-Object System.Drawing.Font $fam, 15, ([System.Drawing.FontStyle]::Regular), $PU

$bTitle  = New-Object System.Drawing.SolidBrush $cTitle
$bSub    = New-Object System.Drawing.SolidBrush $cSub
$bItems  = New-Object System.Drawing.SolidBrush $cItems
$bNote   = New-Object System.Drawing.SolidBrush $cNote
$bFooter = New-Object System.Drawing.SolidBrush $cFooter
$sf = [System.Drawing.StringFormat]::GenericTypographic

# ---------------- helpers ----------------
function New-RoundPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $p.CloseFigure()
    return $p
}

$SEP = ' ' + [char]0x00B7 + ' '   # middle dot separator used in item lists

function Wrap-Text($gfx, [string]$text, $font, [float]$maxWidth, [bool]$splitSegments) {
    # Greedy line packing. ASCII words stay atomic, CJK may break anywhere,
    # and when $splitSegments is set the middle-dot separators are preferred
    # break points so that item lists never break in the middle of an item.
    $segments = @($text)
    if ($splitSegments) {
        $segments = @($text -split ([string][char]0x00B7) | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' })
    }
    $out = New-Object System.Collections.ArrayList
    $cur = ''
    $sepPending = $false
    foreach ($seg in $segments) {
        $segW = $gfx.MeasureString($seg, $font, 10000, $sf).Width
        $joiner = ''
        if ($cur.Length -gt 0 -and $sepPending) { $joiner = $SEP }
        if ($cur.Length -gt 0 -and $segW -le $maxWidth) {
            $need = $gfx.MeasureString($cur + $joiner + $seg, $font, 10000, $sf).Width
            if ($need -gt $maxWidth) {
                # the whole segment fits on a fresh line: move it down instead
                # of splitting it across two lines
                [void]$out.Add($cur.TrimEnd())
                $cur = ''
                $sepPending = $false
            }
        }
        foreach ($m in [regex]::Matches($seg, '[@A-Za-z0-9][A-Za-z0-9@._/:\-+]*|.')) {
            $unit = $m.Value
            $joiner = ''
            if ($cur.Length -gt 0 -and $sepPending) { $joiner = $SEP }
            $cand = $cur + $joiner + $unit
            if ($cur.Length -gt 0 -and $gfx.MeasureString($cand, $font, 10000, $sf).Width -gt $maxWidth) {
                [void]$out.Add($cur.TrimEnd())
                $cur = $unit.TrimStart()
            } else {
                $cur = $cand
            }
            $sepPending = $false
        }
        $sepPending = $true
    }
    if ($cur.Trim().Length -gt 0) { [void]$out.Add($cur.TrimEnd()) }
    return $out
}

function Draw-Lines($gfx, $lines, $font, $brush, [float]$x, [float]$y, [float]$lh) {
    $yy = $y
    foreach ($ln in $lines) {
        $gfx.DrawString($ln, $font, $brush, $x, $yy, $sf)
        $yy += $lh
    }
    return $yy
}

function Draw-Head($gfx, [float]$cx, [float]$cy, [float]$dir, $pen, $brush) {
    $h = 8.0; $w = 5.0
    $pts = @(
        (New-Object System.Drawing.PointF $cx, ($cy + $dir * $h)),
        (New-Object System.Drawing.PointF ($cx - $w), ($cy - $dir * $h * 0.15)),
        (New-Object System.Drawing.PointF ($cx + $w), ($cy - $dir * $h * 0.15))
    )
    $gfx.DrawPolygon($pen, $pts)
    $gfx.FillPolygon($brush, $pts)
}

# ---------------- title ----------------
$accentPath = New-RoundPath $padX ($padTop + 6) 9 38 4
$bAccent = New-Object System.Drawing.SolidBrush (Col '#2E6DA4')
$g.FillPath($bAccent, $accentPath)
$g.DrawString($data.title, $fTitle, $bTitle, ($padX + 22), ($padTop + 2), $sf)
$g.DrawString($data.subtitle, $fSub, $bSub, ($padX + 23), ($padTop + 42), $sf)

function Draw-LinesCenter($gfx, $lines, $font, $brush, [float]$centerX, [float]$y, [float]$lh) {
    $yy = $y
    foreach ($ln in $lines) {
        $w = $gfx.MeasureString($ln, $font, 10000, $sf).Width
        $gfx.DrawString($ln, $font, $brush, ($centerX - $w / 2), $yy, $sf)
        $yy += $lh
    }
    return $yy
}

# ---------------- sideband ----------------
$bandPath = New-RoundPath $bandX $bodyTop $bandW $bodyH 12
$bBandFill = New-Object System.Drawing.SolidBrush (Col '#F4F8FC')
$pBand = New-Object System.Drawing.Pen (Col '#A9C4DC'), 1.4
$pBand.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash
$g.FillPath($bBandFill, $bandPath)
$g.DrawPath($pBand, $bandPath)

$bandCenter = $bandX + $bandW / 2
$bBandT = New-Object System.Drawing.SolidBrush (Col '#2E6DA4')
$bBandL = New-Object System.Drawing.SolidBrush (Col '#3A5F82')
$titleLines = @(Wrap-Text $g $data.sideband.title $fBandT ($bandW - 34) $false)
$ty = Draw-LinesCenter $g $titleLines $fBandT $bBandT $bandCenter ($bodyTop + 22) 24

$pBandRule = New-Object System.Drawing.Pen (Col '#C6D9E9'), 1.1
$g.DrawLine($pBandRule, ($bandX + 24), ($ty + 8), ($bandX + $bandW - 24), ($ty + 8))

$bandItems = @($data.sideband.lines)
$slotTop = $ty + 26
$slotBottom = $bodyTop + $bodyH - 22
$slotH = ($slotBottom - $slotTop) / $bandItems.Count

# dotted spine behind the tags, to read as "cross-cutting through all layers"
$pSpine = New-Object System.Drawing.Pen (Col '#C6D9E9'), 1.3
$pSpine.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dot
$g.DrawLine($pSpine, $bandCenter, ($ty + 14), $bandCenter, ($slotBottom - 12))

for ($i = 0; $i -lt $bandItems.Count; $i++) {
    $lines = @(Wrap-Text $g $bandItems[$i] $fBandL ($bandW - 46) $false)
    $chipH = 24 + ($lines.Count - 1) * 21
    $chipY = $slotTop + $i * $slotH + ($slotH - $chipH) / 2
    $itemPath = New-RoundPath ($bandX + 14) $chipY ($bandW - 28) $chipH 8
    $bItemFill = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
    $pItem = New-Object System.Drawing.Pen (Col '#BFD5E8'), 1.1
    $g.FillPath($bItemFill, $itemPath)
    $g.DrawPath($pItem, $itemPath)
    [void](Draw-LinesCenter $g $lines $fBandL $bBandL $bandCenter ($chipY + 10) 21)
}

# ---------------- layer rows ----------------
$rows = @()
for ($i = 0; $i -lt $n; $i++) {
    $y = $bodyTop + $i * ($rowH + $rowGap)
    $rows += [pscustomobject]@{ index = $i; y = $y; layer = $layers[$i] }
}

foreach ($row in $rows) {
    $layer = $row.layer
    $acc = [string]$layer.accent
    if (-not $chip.ContainsKey($acc)) { $acc = 'normal' }
    $y = $row.y

    # label chip
    $chipPath = New-RoundPath $padX $y $chipW $rowH 10
    $bChipFill = New-Object System.Drawing.SolidBrush $chip[$acc].fill
    $pChip = New-Object System.Drawing.Pen $chip[$acc].line, 1.8
    $g.FillPath($bChipFill, $chipPath)
    $g.DrawPath($pChip, $chipPath)
    $bChipText = New-Object System.Drawing.SolidBrush $chip[$acc].text
    $tw = $g.MeasureString($layer.label, $fChip, 10000, $sf).Width
    $g.DrawString($layer.label, $fChip, $bChipText, ($padX + ($chipW - $tw) / 2), ($y + ($rowH - 34) / 2), $sf)

    # content box
    $boxPath = New-RoundPath $contentX $y $contentW $rowH 10
    $bBoxFill = New-Object System.Drawing.SolidBrush $box[$acc].fill
    $pBox = New-Object System.Drawing.Pen $box[$acc].line, 1.8
    $g.FillPath($bBoxFill, $boxPath)
    $g.DrawPath($pBox, $boxPath)

    $itemLines = Wrap-Text $g $layer.items $fItems ($contentW - 44) $true
    $noteLines = Wrap-Text $g $layer.note $fNote ($contentW - 44) $false
    $itemH = $itemLines.Count * 32
    $noteH = $noteLines.Count * 23
    $blockH = $itemH + 12 + $noteH
    $ty = $y + ($rowH - $blockH) / 2
    $ty = Draw-Lines $g $itemLines $fItems $bItems ($contentX + 22) $ty 32
    $ty += 12
    [void](Draw-Lines $g $noteLines $fNote $bNote ($contentX + 22) $ty 23)
}

# ---------------- arrows between rows ----------------
$arrowX = $contentX + $contentW / 2
$pArrow = New-Object System.Drawing.Pen $cArrow, 2.6
$bArrow = New-Object System.Drawing.SolidBrush $cArrow
$bArrowNote = New-Object System.Drawing.SolidBrush (Col '#5B7085')
for ($i = 0; $i -lt $n - 1; $i++) {
    $top = $rows[$i].y + $rowH
    $bottom = $rows[$i + 1].y
    if ($i -eq 0) {
        # front end <-> back end: request down, response up
        $g.DrawLine($pArrow, $arrowX, ($top + 2), $arrowX, ($bottom - 2))
        Draw-Head $g $arrowX $bottom 1 $pArrow $bArrow
        Draw-Head $g $arrowX $top -1 $pArrow $bArrow
        $g.DrawString($data.arrowLabel, $fNote, $bArrowNote, ($arrowX + 16), ($top + ($bottom - $top) / 2 - 10), $sf)
    } else {
        $g.DrawLine($pArrow, $arrowX, ($top + 2), $arrowX, ($bottom - 8))
        Draw-Head $g $arrowX $bottom 1 $pArrow $bArrow
    }
}

# ---------------- footer ----------------
$ruleY = $bodyTop + $bodyH + 30
$pRule = New-Object System.Drawing.Pen $cRule, 1.2
$g.DrawLine($pRule, $padX, $ruleY, ($W - $padX), $ruleY)
$footLines = Wrap-Text $g $data.footer $fFoot ($W - 2 * $padX - 10) $false
[void](Draw-Lines $g $footLines $fFoot $bFooter $padX ($ruleY + 14) 23)

# ---------------- save ----------------
$g.Dispose()
$outPath = Join-Path $PSScriptRoot $data.output
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output ('saved: ' + $outPath)

if ($data.extraCopyDir -and (Test-Path $data.extraCopyDir)) {
    $copyPath = Join-Path $data.extraCopyDir $data.output
    Copy-Item -LiteralPath $outPath -Destination $copyPath -Force
    Write-Output ('copied: ' + $copyPath)
}
