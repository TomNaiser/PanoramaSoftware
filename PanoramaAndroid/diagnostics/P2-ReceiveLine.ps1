function Select-P2FrameSuffix([string]$Line) {
    # A reopened serial stream may contain a prefix from an unfinished frame.
    # Resynchronize at the last complete start marker; callers still validate
    # CRC and fields before accepting any response.
    $start=$Line.LastIndexOf('@P2|',[StringComparison]::Ordinal)
    if($start -ge 0){return $Line.Substring($start)}
    return $Line
}
