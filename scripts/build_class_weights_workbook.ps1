param(
    [string]$CsvDir = "docs/class_weights_overview",
    [string]$OutputPath = "docs/class_weights_overview/class_weights_overview.xlsm",
    [switch]$Open
)

$ErrorActionPreference = "Stop"

function Release-ComObject {
    param([object]$ComObject)
    if ($null -ne $ComObject) {
        try {
            [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($ComObject)
        } catch {
        }
    }
}

function Get-ExcelColumnName {
    param([int]$ColumnNumber)
    $name = ""
    $n = $ColumnNumber
    while ($n -gt 0) {
        $remainder = [int](($n - 1) % 26)
        $name = ([char]([int](65 + $remainder))).ToString() + $name
        $n = [math]::Floor(($n - 1) / 26)
    }
    return $name
}

function Import-CsvAutoDelimiter {
    param([string]$Path)

    $firstLine = Get-Content -Path $Path -TotalCount 1 -Encoding UTF8
    if ($null -eq $firstLine) {
        return @()
    }

    $commaCount = ([regex]::Matches($firstLine, ",")).Count
    $semicolonCount = ([regex]::Matches($firstLine, ";")).Count
    $delimiter = if ($semicolonCount -gt $commaCount) { ';' } else { ',' }

    return Import-Csv -Path $Path -Delimiter $delimiter
}

function Add-ClassViewSheet {
    param([object]$Workbook)

    $sheet = $Workbook.Worksheets.Add()
    $sheet.Name = "class_view"

    $sheet.Range("A1").Value2 = "Class Overview"
    $sheet.Range("A1").Font.Bold = $true
    $sheet.Range("A1").Font.Size = 16

    $sheet.Range("A3").Value2 = "Selected class"
    $sheet.Range("A3").Font.Bold = $true
    $sheet.Range("B3").Value2 = "tanker"
    $sheet.Range("B3").Interior.Color = 0xFFF2CC
    $sheet.Range("Q1").Value2 = "weight"
    $sheet.Range("Q2").Value2 = "name"
    $sheet.Range("G3").Value2 = "Attr sort"
    $sheet.Range("G3").Font.Bold = $true
    $sheet.Range("H3").Value2 = "weight"
    $sheet.Range("K3").Value2 = "Ability sort"
    $sheet.Range("K3").Font.Bold = $true
    $sheet.Range("L3").Value2 = "weight"

    $sheet.Range("A5").Value2 = "Categories"
    $sheet.Range("A5").Font.Bold = $true
    $sheet.Range("A6").Value2 = "base"
    $sheet.Range("A7").Value2 = "attribute"
    $sheet.Range("A8").Value2 = "ability"
    $sheet.Range("A9").Value2 = "pref attr mult"
    $sheet.Range("A10").Value2 = "pref ability mult"
    $sheet.Range("B6:B8").NumberFormat = "0.0\%"

    $baseSheet = $Workbook.Worksheets.Item("base_stats")
    $baseLastRow = $baseSheet.UsedRange.Rows.Count
    $baseHeaderCols = $baseSheet.UsedRange.Columns.Count
    $baseLastCol = Get-ExcelColumnName $baseHeaderCols
    $baseDataRows = [Math]::Max(0, $baseHeaderCols - 1)
    $sheet.Range("D5").Value2 = "Base Stats"
    $sheet.Range("D5").Font.Bold = $true
    $sheet.Range("E5").Value2 = "Weight"
    $sheet.Range("E5").Font.Bold = $true
    for ($i = 0; $i -lt $baseDataRows; $i++) {
        $targetRow = 6 + $i
        $columnIndex = 2 + $i
        $sheet.Cells.Item($targetRow, 4).Formula = '=INDEX(base_stats!$1:$1,' + $columnIndex + ')'
        $sheet.Cells.Item($targetRow, 5).Value2 = ""
    }

    $attrSheet = $Workbook.Worksheets.Item("attributes")
    $attrLastRow = $attrSheet.UsedRange.Rows.Count
    $attrHeaderCols = $attrSheet.UsedRange.Columns.Count
    $attrLastCol = Get-ExcelColumnName $attrHeaderCols
    $attrDataRows = [Math]::Max(0, $attrHeaderCols - 1)
    $sheet.Range("G5").Value2 = "Attributes"
    $sheet.Range("G5").Font.Bold = $true
    $sheet.Range("H5").Value2 = "Weight"
    $sheet.Range("H5").Font.Bold = $true
    $sheet.Range("I5").Value2 = "First %"
    $sheet.Range("I5").Font.Bold = $true
    for ($i = 0; $i -lt $attrDataRows; $i++) {
        $targetRow = 6 + $i
        $columnIndex = 2 + $i
        $sheet.Cells.Item($targetRow, 7).Formula = '=INDEX(attributes!$1:$1,' + $columnIndex + ')'
        $sheet.Cells.Item($targetRow, 8).Value2 = ""
    }

    $abilitySheet = $Workbook.Worksheets.Item("abilities")
    $abilityLastRow = $abilitySheet.UsedRange.Rows.Count
    $abilityHeaderCols = $abilitySheet.UsedRange.Columns.Count
    $abilityLastCol = Get-ExcelColumnName $abilityHeaderCols
    $abilityDataRows = [Math]::Max(0, $abilityHeaderCols - 1)
    $sheet.Range("K5").Value2 = "Abilities"
    $sheet.Range("K5").Font.Bold = $true
    $sheet.Range("L5").Value2 = "Weight"
    $sheet.Range("L5").Font.Bold = $true
    $sheet.Range("M5").Value2 = "First %"
    $sheet.Range("M5").Font.Bold = $true
    for ($i = 0; $i -lt $abilityDataRows; $i++) {
        $targetRow = 6 + $i
        $columnIndex = 2 + $i
        $sheet.Cells.Item($targetRow, 11).Formula = '=INDEX(abilities!$1:$1,' + $columnIndex + ')'
        $sheet.Cells.Item($targetRow, 12).Value2 = ""
    }

    $metaSheet = $Workbook.Worksheets.Item("meta")
    $sheet.Range("O5").Value2 = "Meta"
    $sheet.Range("O5").Font.Bold = $true
    $sheet.Range("O6").Value2 = "default preferredAttributeWeightMultiplier"
    $sheet.Range("O7").Value2 = "default preferredAbilityWeightMultiplier"
    $sheet.Range("P6").Formula = '=IFERROR(VLOOKUP("preferredAttributeWeightMultiplier",meta!$A:$B,2,FALSE),"")'
    $sheet.Range("P7").Formula = '=IFERROR(VLOOKUP("preferredAbilityWeightMultiplier",meta!$A:$B,2,FALSE),"")'
    $sheet.Range("O10").Value2 = "Rough Risk"
    $sheet.Range("O10").Font.Bold = $true
    $sheet.Range("O11").Value2 = "non-pref ability by 100"
    $sheet.Range("O12").Value2 = "non-pref ability by 200"
    $sheet.Range("O13").Value2 = "non-pref attribute by 100"
    $sheet.Range("O14").Value2 = "non-pref attribute by 200"
    $sheet.Range("P11").Formula = '=IFERROR(RoughRisk($B$8,$B$6:$B$8,$L$6:$L$' + (5 + $abilityDataRows) + ',$B$10,99),0)'
    $sheet.Range("P12").Formula = '=IFERROR(RoughRisk($B$8,$B$6:$B$8,$L$6:$L$' + (5 + $abilityDataRows) + ',$B$10,199),0)'
    $sheet.Range("P13").Formula = '=IFERROR(RoughRisk($B$7,$B$6:$B$8,$H$6:$H$' + (5 + $attrDataRows) + ',$B$9,99),0)'
    $sheet.Range("P14").Formula = '=IFERROR(RoughRisk($B$7,$B$6:$B$8,$H$6:$H$' + (5 + $attrDataRows) + ',$B$9,199),0)'

    $defaultClass = "tanker"
    for ($row = 2; $row -le $Workbook.Worksheets.Item("categories").UsedRange.Rows.Count; $row++) {
        if ($Workbook.Worksheets.Item("categories").Cells.Item($row, 1).Value2 -eq $defaultClass) {
            $sheet.Range("B6").Value2 = $Workbook.Worksheets.Item("categories").Cells.Item($row, 2).Value2
            $sheet.Range("B7").Value2 = $Workbook.Worksheets.Item("categories").Cells.Item($row, 3).Value2
            $sheet.Range("B8").Value2 = $Workbook.Worksheets.Item("categories").Cells.Item($row, 4).Value2
            $sheet.Range("B9").Value2 = $Workbook.Worksheets.Item("categories").Cells.Item($row, 5).Value2
            $sheet.Range("B10").Value2 = $Workbook.Worksheets.Item("categories").Cells.Item($row, 6).Value2
            break
        }
    }

    for ($i = 0; $i -lt $baseDataRows; $i++) {
        $sourceCol = 2 + $i
        $sheet.Cells.Item(6 + $i, 5).Formula = '=INDEX(base_stats!$A$2:$' + $baseLastCol + '$' + $baseLastRow + ',MATCH($B$3,base_stats!$A$2:$A$' + $baseLastRow + ',0),' + $sourceCol + ')'
    }
    for ($i = 0; $i -lt $attrDataRows; $i++) {
        $sourceCol = 2 + $i
        $sheet.Cells.Item(6 + $i, 8).Formula = '=INDEX(attributes!$A$2:$' + $attrLastCol + '$' + $attrLastRow + ',MATCH($B$3,attributes!$A$2:$A$' + $attrLastRow + ',0),' + $sourceCol + ')'
        $attrEndRow = 5 + $attrDataRows
        $sheet.Cells.Item(6 + $i, 9).Formula = '=IF($H' + (6 + $i) + '="","",IFERROR((($H' + (6 + $i) + '/10)*(1+($H' + (6 + $i) + '>10)*($B$9-1)))/SUMPRODUCT(($H$6:$H$' + $attrEndRow + '/10)*(1+($H$6:$H$' + $attrEndRow + '>10)*($B$9-1))),0))'
    }
    for ($i = 0; $i -lt $abilityDataRows; $i++) {
        $sourceCol = 2 + $i
        $sheet.Cells.Item(6 + $i, 12).Formula = '=INDEX(abilities!$A$2:$' + $abilityLastCol + '$' + $abilityLastRow + ',MATCH($B$3,abilities!$A$2:$A$' + $abilityLastRow + ',0),' + $sourceCol + ')'
        $abilityEndRow = 5 + $abilityDataRows
        $sheet.Cells.Item(6 + $i, 13).Formula = '=IF($L' + (6 + $i) + '="","",IFERROR((($L' + (6 + $i) + '/10)*(1+($L' + (6 + $i) + '>10)*($B$10-1)))/SUMPRODUCT(($L$6:$L$' + $abilityEndRow + '/10)*(1+($L$6:$L$' + $abilityEndRow + '>10)*($B$10-1))),0))'
    }

    $sheet.Range("A3:B3").Interior.Color = 0xFFF2CC
    $sheet.Range("A5:B10").Borders.LineStyle = 1
    $sheet.Range("D5:E40").Borders.LineStyle = 1
    $sheet.Range("G5:I120").Borders.LineStyle = 1
    $sheet.Range("K5:M120").Borders.LineStyle = 1
    $sheet.Range("O5:P14").Borders.LineStyle = 1

    $sheet.Columns.Item("A").ColumnWidth = 18
    $sheet.Columns.Item("B").ColumnWidth = 16
    $sheet.Columns.Item("D").ColumnWidth = 24
    $sheet.Columns.Item("E").ColumnWidth = 12
    $sheet.Columns.Item("G").ColumnWidth = 28
    $sheet.Columns.Item("H").ColumnWidth = 12
    $sheet.Columns.Item("I").ColumnWidth = 11
    $sheet.Columns.Item("J").ColumnWidth = 4
    $sheet.Columns.Item("K").ColumnWidth = 28
    $sheet.Columns.Item("L").ColumnWidth = 12
    $sheet.Columns.Item("M").ColumnWidth = 11
    $sheet.Columns.Item("N").ColumnWidth = 4
    $sheet.Columns.Item("O").ColumnWidth = 34
    $sheet.Columns.Item("P").ColumnWidth = 14

    $sheet.Range("E6:E40").NumberFormat = "0"
    $sheet.Range("B9:B10").NumberFormat = "0.0###"
    $sheet.Range("H6:H120").NumberFormat = "0"
    $sheet.Range("I6:I120").NumberFormat = "0.0%"
    $sheet.Range("L6:L120").NumberFormat = "0"
    $sheet.Range("M6:M120").NumberFormat = "0.0%"
    $sheet.Range("P6:P7").NumberFormat = "0.0###"
    $sheet.Range("P11:P14").NumberFormat = "0.0%"

    $validation = $sheet.Range("B3").Validation
    $validation.Delete()
    $categoriesLastRow = $Workbook.Worksheets.Item("categories").UsedRange.Rows.Count
    $validation.Add(3, 1, 1, ("=categories!`$A`$2:`$A`$$categoriesLastRow"))
    $validation.IgnoreBlank = $true
    $validation.InCellDropdown = $true
    $validation.ShowError = $false

    $sortValidationAttributes = $sheet.Range("H3").Validation
    $sortValidationAttributes.Delete()
    $sortValidationAttributes.Add(3, 1, 1, '=class_view!$Q$1:$Q$2')
    $sortValidationAttributes.IgnoreBlank = $true
    $sortValidationAttributes.InCellDropdown = $true
    $sortValidationAttributes.ShowError = $false

    $sortValidationAbilities = $sheet.Range("L3").Validation
    $sortValidationAbilities.Delete()
    $sortValidationAbilities.Add(3, 1, 1, '=class_view!$Q$1:$Q$2')
    $sortValidationAbilities.IgnoreBlank = $true
    $sortValidationAbilities.InCellDropdown = $true
    $sortValidationAbilities.ShowError = $false

    $sheet.Columns.Item("Q").Hidden = $true

    $applyButton = $sheet.Shapes.AddShape(1, 260, 12, 150, 28)
    $applyButton.TextFrame.Characters().Text = "Apply Edits To JSON"
    $applyButton.Fill.ForeColor.RGB = 0xC9DAF8
    $applyButton.OnAction = "ApplyClassViewEdits"

    $reloadButton = $sheet.Shapes.AddShape(1, 420, 12, 120, 28)
    $reloadButton.TextFrame.Characters().Text = "Reload Class"
    $reloadButton.Fill.ForeColor.RGB = 0xD9EAD3
    $reloadButton.OnAction = "LoadSelectedClassView"

    $newClassButton = $sheet.Shapes.AddShape(1, 550, 12, 120, 28)
    $newClassButton.TextFrame.Characters().Text = "New Class"
    $newClassButton.Fill.ForeColor.RGB = 0xFCE5CD
    $newClassButton.OnAction = "NewClassView"

    try {
        $sheet.Calculate()
    } catch {
    }

    $sheet.Activate() | Out-Null
    return $sheet
}

function Add-VbaMacroModule {
    param([object]$Workbook)

    $vbaCode = @'
Option Explicit

Private Function FindClassRow(ws As Worksheet, className As String) As Long
    Dim lastRow As Long
    Dim r As Long
    lastRow = ws.Cells(ws.Rows.Count, 1).End(xlUp).Row
    For r = 2 To lastRow
        If LCase$(Trim$(CStr(ws.Cells(r, 1).Value2))) = LCase$(Trim$(className)) Then
            FindClassRow = r
            Exit Function
        End If
    Next r
    FindClassRow = 0
End Function

Private Function FindHeaderColumn(ws As Worksheet, headerName As String) As Long
    Dim lastCol As Long
    Dim c As Long
    lastCol = ws.Cells(1, ws.Columns.Count).End(xlToLeft).Column
    For c = 2 To lastCol
        If LCase$(Trim$(CStr(ws.Cells(1, c).Value2))) = LCase$(Trim$(headerName)) Then
            FindHeaderColumn = c
            Exit Function
        End If
    Next c
    FindHeaderColumn = 0
End Function

Private Function EnsureClassRow(ws As Worksheet, className As String, Optional defaultValue As Double = 10) As Long
    Dim rowIndex As Long, lastRow As Long, lastCol As Long, c As Long
    rowIndex = FindClassRow(ws, className)
    If rowIndex > 0 Then
        EnsureClassRow = rowIndex
        Exit Function
    End If

    lastRow = ws.Cells(ws.Rows.Count, 1).End(xlUp).Row + 1
    ws.Cells(lastRow, 1).Value2 = className
    lastCol = ws.Cells(1, ws.Columns.Count).End(xlToLeft).Column
    For c = 2 To lastCol
        ws.Cells(lastRow, c).Value2 = defaultValue
    Next c
    EnsureClassRow = lastRow
End Function

Private Function MetaValue(wsMeta As Worksheet, metaKey As String, Optional fallbackValue As Double = 0) As Double
    Dim lastRow As Long, r As Long
    lastRow = wsMeta.Cells(wsMeta.Rows.Count, 1).End(xlUp).Row
    For r = 2 To lastRow
        If LCase$(Trim$(CStr(wsMeta.Cells(r, 1).Value2))) = LCase$(Trim$(metaKey)) Then
            If IsNumeric(wsMeta.Cells(r, 2).Value2) Then
                MetaValue = CDbl(wsMeta.Cells(r, 2).Value2)
                Exit Function
            End If
        End If
    Next r
    MetaValue = fallbackValue
End Function

Private Sub SortSection(viewWs As Worksheet, labelCol As Long, valueCol As Long, ByVal sortByWeight As Boolean)
    Dim startRow As Long, endRow As Long, i As Long, j As Long
    Dim labelA As String, labelB As String, valueA As Double, valueB As Double
    Dim tmpLabel As Variant, tmpValue As Variant

    startRow = 6
    endRow = startRow
    Do While Trim$(CStr(viewWs.Cells(endRow, labelCol).Value2)) <> ""
        endRow = endRow + 1
    Loop
    endRow = endRow - 1
    If endRow <= startRow Then Exit Sub

    For i = startRow To endRow - 1
        For j = i + 1 To endRow
            labelA = LCase$(Trim$(CStr(viewWs.Cells(i, labelCol).Value2)))
            labelB = LCase$(Trim$(CStr(viewWs.Cells(j, labelCol).Value2)))
            valueA = 0
            valueB = 0
            If IsNumeric(viewWs.Cells(i, valueCol).Value2) Then valueA = CDbl(viewWs.Cells(i, valueCol).Value2)
            If IsNumeric(viewWs.Cells(j, valueCol).Value2) Then valueB = CDbl(viewWs.Cells(j, valueCol).Value2)

            Dim shouldSwap As Boolean
            shouldSwap = False
            If sortByWeight Then
                If valueB > valueA Then
                    shouldSwap = True
                ElseIf valueB = valueA And labelB < labelA Then
                    shouldSwap = True
                End If
            Else
                If labelB < labelA Then
                    shouldSwap = True
                End If
            End If

            If shouldSwap Then
                tmpLabel = viewWs.Cells(i, labelCol).Value2
                tmpValue = viewWs.Cells(i, valueCol).Value2
                viewWs.Cells(i, labelCol).Value2 = viewWs.Cells(j, labelCol).Value2
                viewWs.Cells(i, valueCol).Value2 = viewWs.Cells(j, valueCol).Value2
                viewWs.Cells(j, labelCol).Value2 = tmpLabel
                viewWs.Cells(j, valueCol).Value2 = tmpValue
            End If
        Next j
    Next i
End Sub

Private Function WantsWeightSort(cellValue As Variant, Optional defaultWeight As Boolean = True) As Boolean
    Dim normalized As String
    normalized = LCase$(Trim$(CStr(cellValue)))
    If normalized = "" Then
        WantsWeightSort = defaultWeight
        Exit Function
    End If
    WantsWeightSort = (normalized <> "name")
End Function

Private Function PreferredShare(weightRange As Range, preferredMultiplier As Double) As Double
    Dim cell As Range
    Dim rawWeight As Double, effectiveWeight As Double
    Dim preferredWeight As Double, totalWeight As Double

    preferredWeight = 0#
    totalWeight = 0#
    For Each cell In weightRange.Cells
        If IsNumeric(cell.Value2) Then
            rawWeight = CDbl(cell.Value2) / 10#
            effectiveWeight = rawWeight
            If rawWeight > 1# Then
                effectiveWeight = rawWeight * preferredMultiplier
                preferredWeight = preferredWeight + effectiveWeight
            End If
            totalWeight = totalWeight + effectiveWeight
        End If
    Next cell

    If totalWeight <= 0# Then
        PreferredShare = 0#
    Else
        PreferredShare = preferredWeight / totalWeight
    End If
End Function

Public Function RoughRisk(categoryWeight As Double, categoryRange As Range, weightRange As Range, preferredMultiplier As Double, rolls As Long) As Double
    Dim totalCategory As Double
    Dim categoryChance As Double
    Dim preferredShareValue As Double
    Dim nonPreferredPerRoll As Double

    totalCategory = 0#
    totalCategory = Application.WorksheetFunction.Sum(categoryRange)
    If totalCategory <= 0# Or rolls <= 0 Then
        RoughRisk = 0#
        Exit Function
    End If

    categoryChance = categoryWeight / totalCategory
    preferredShareValue = PreferredShare(weightRange, preferredMultiplier)
    nonPreferredPerRoll = categoryChance * (1# - preferredShareValue)
    If nonPreferredPerRoll <= 0# Then
        RoughRisk = 0#
        Exit Function
    End If

    RoughRisk = 1# - ((1# - nonPreferredPerRoll) ^ rolls)
End Function

Public Sub LoadSelectedClassView()
    Dim wsView As Worksheet, wsCategories As Worksheet, wsBase As Worksheet, wsAttr As Worksheet, wsAbilities As Worksheet
    Dim className As String, classRow As Long, r As Long, sourceCol As Long

    Set wsView = ThisWorkbook.Worksheets("class_view")
    Set wsCategories = ThisWorkbook.Worksheets("categories")
    Set wsBase = ThisWorkbook.Worksheets("base_stats")
    Set wsAttr = ThisWorkbook.Worksheets("attributes")
    Set wsAbilities = ThisWorkbook.Worksheets("abilities")

    className = Trim$(CStr(wsView.Range("B3").Value2))
    If className = "" Then Exit Sub

    classRow = FindClassRow(wsCategories, className)
    If classRow > 0 Then
        wsView.Range("B6").Value2 = wsCategories.Cells(classRow, 2).Value2
        wsView.Range("B7").Value2 = wsCategories.Cells(classRow, 3).Value2
        wsView.Range("B8").Value2 = wsCategories.Cells(classRow, 4).Value2
        If IsNumeric(wsCategories.Cells(classRow, 5).Value2) Then
            wsView.Range("B9").Value2 = wsCategories.Cells(classRow, 5).Value2
        Else
            wsView.Range("B9").Value2 = MetaValue(ThisWorkbook.Worksheets("meta"), "preferredAttributeWeightMultiplier", 1)
        End If
        If IsNumeric(wsCategories.Cells(classRow, 6).Value2) Then
            wsView.Range("B10").Value2 = wsCategories.Cells(classRow, 6).Value2
        Else
            wsView.Range("B10").Value2 = MetaValue(ThisWorkbook.Worksheets("meta"), "preferredAbilityWeightMultiplier", 1)
        End If
    End If

    r = 6
    Do While Trim$(CStr(wsView.Cells(r, 4).Value2)) <> ""
        sourceCol = FindHeaderColumn(wsBase, CStr(wsView.Cells(r, 4).Value2))
        If sourceCol > 0 Then
            classRow = FindClassRow(wsBase, className)
            If classRow > 0 Then wsView.Cells(r, 5).Value2 = wsBase.Cells(classRow, sourceCol).Value2
        End If
        r = r + 1
    Loop

    r = 6
    Do While Trim$(CStr(wsView.Cells(r, 7).Value2)) <> ""
        sourceCol = FindHeaderColumn(wsAttr, CStr(wsView.Cells(r, 7).Value2))
        If sourceCol > 0 Then
            classRow = FindClassRow(wsAttr, className)
            If classRow > 0 Then wsView.Cells(r, 8).Value2 = wsAttr.Cells(classRow, sourceCol).Value2
        End If
        r = r + 1
    Loop

    r = 6
    Do While Trim$(CStr(wsView.Cells(r, 11).Value2)) <> ""
        sourceCol = FindHeaderColumn(wsAbilities, CStr(wsView.Cells(r, 11).Value2))
        If sourceCol > 0 Then
            classRow = FindClassRow(wsAbilities, className)
            If classRow > 0 Then wsView.Cells(r, 12).Value2 = wsAbilities.Cells(classRow, sourceCol).Value2
        End If
        r = r + 1
    Loop

    SortSection wsView, 4, 5, True
    SortSection wsView, 7, 8, WantsWeightSort(wsView.Range("H3").Value2, True)
    SortSection wsView, 11, 12, WantsWeightSort(wsView.Range("L3").Value2, True)
End Sub

Public Sub NewClassView()
    Dim wsView As Worksheet, wsMeta As Worksheet
    Dim r As Long

    Set wsView = ThisWorkbook.Worksheets("class_view")
    Set wsMeta = ThisWorkbook.Worksheets("meta")

    wsView.Range("B3").Value2 = ""
    wsView.Range("H3").Value2 = "name"
    wsView.Range("L3").Value2 = "name"
    wsView.Range("B6").Value2 = MetaValue(wsMeta, "defaultCategory.base", 90)
    wsView.Range("B7").Value2 = MetaValue(wsMeta, "defaultCategory.attribute", 10)
    wsView.Range("B8").Value2 = MetaValue(wsMeta, "defaultCategory.ability", 10)
    wsView.Range("B9").Value2 = MetaValue(wsMeta, "preferredAttributeWeightMultiplier", 1)
    wsView.Range("B10").Value2 = MetaValue(wsMeta, "preferredAbilityWeightMultiplier", 1)

    r = 6
    Do While Trim$(CStr(wsView.Cells(r, 4).Value2)) <> ""
        wsView.Cells(r, 5).Value2 = 10
        r = r + 1
    Loop

    r = 6
    Do While Trim$(CStr(wsView.Cells(r, 7).Value2)) <> ""
        wsView.Cells(r, 8).Value2 = 10
        r = r + 1
    Loop

    r = 6
    Do While Trim$(CStr(wsView.Cells(r, 11).Value2)) <> ""
        wsView.Cells(r, 12).Value2 = 10
        r = r + 1
    Loop

    SortSection wsView, 4, 5, False
    SortSection wsView, 7, 8, WantsWeightSort(wsView.Range("H3").Value2, False)
    SortSection wsView, 11, 12, WantsWeightSort(wsView.Range("L3").Value2, False)
End Sub

Private Sub WriteClassValues(viewWs As Worksheet, dataWs As Worksheet, labelCol As Long, valueCol As Long, className As String)
    Dim classRow As Long, r As Long, headerName As String, targetCol As Long
    classRow = EnsureClassRow(dataWs, className, 10)

    r = 6
    Do While Trim$(CStr(viewWs.Cells(r, labelCol).Value2)) <> ""
        headerName = Trim$(CStr(viewWs.Cells(r, labelCol).Value2))
        targetCol = FindHeaderColumn(dataWs, headerName)
        If targetCol > 0 Then
            dataWs.Cells(classRow, targetCol).Value2 = viewWs.Cells(r, valueCol).Value2
        End If
        r = r + 1
    Loop
End Sub

Private Function CsvCellText(cellValue As Variant, ByVal scaleDown As Boolean) As String
    Dim numberValue As Double, textValue As String
    If IsNumeric(cellValue) Then
        numberValue = CDbl(cellValue)
        If scaleDown Then
            numberValue = numberValue / 10#
        End If
        textValue = CStr(numberValue)
        textValue = Replace(textValue, Application.DecimalSeparator, ".")
        CsvCellText = textValue
        Exit Function
    End If

    textValue = CStr(cellValue)
    textValue = Replace(textValue, """", """""")
    If InStr(textValue, ",") > 0 Or InStr(textValue, """") > 0 Or InStr(textValue, vbLf) > 0 Then
        textValue = """" & textValue & """"
    End If
    CsvCellText = textValue
End Function

Private Sub ExportSheetCsv(ws As Worksheet, outPath As String)
    Dim fso As Object, stream As Object
    Dim lastRow As Long, lastCol As Long, r As Long, c As Long
    Dim line As String, valueText As String, scaleDown As Boolean

    Set fso = CreateObject("Scripting.FileSystemObject")
    Set stream = fso.CreateTextFile(outPath, True, True)
    scaleDown = (ws.Name = "base_stats" Or ws.Name = "attributes" Or ws.Name = "abilities")

    lastRow = ws.Cells(ws.Rows.Count, 1).End(xlUp).Row
    lastCol = ws.Cells(1, ws.Columns.Count).End(xlToLeft).Column

    For r = 1 To lastRow
        line = ""
        For c = 1 To lastCol
            valueText = CsvCellText(ws.Cells(r, c).Value2, scaleDown And c > 1)
            If c > 1 Then line = line & ","
            line = line & valueText
        Next c
        stream.WriteLine line
    Next r

    stream.Close
End Sub

Public Sub ApplyClassViewEdits()
    Dim wsView As Worksheet, wsCategories As Worksheet, wsBase As Worksheet, wsAttr As Worksheet, wsAbilities As Worksheet, wsMeta As Worksheet
    Dim className As String, classRow As Long
    Dim csvDir As String, repoRoot As String, command As String
    Dim shell As Object, exitCode As Long

    Set wsView = ThisWorkbook.Worksheets("class_view")
    Set wsCategories = ThisWorkbook.Worksheets("categories")
    Set wsBase = ThisWorkbook.Worksheets("base_stats")
    Set wsAttr = ThisWorkbook.Worksheets("attributes")
    Set wsAbilities = ThisWorkbook.Worksheets("abilities")
    Set wsMeta = ThisWorkbook.Worksheets("meta")

    className = Trim$(CStr(wsView.Range("B3").Value2))
    If className = "" Then
        MsgBox "Select a class first.", vbExclamation
        Exit Sub
    End If

    classRow = EnsureClassRow(wsCategories, className, 0)

    wsCategories.Cells(classRow, 2).Value2 = wsView.Range("B6").Value2
    wsCategories.Cells(classRow, 3).Value2 = wsView.Range("B7").Value2
    wsCategories.Cells(classRow, 4).Value2 = wsView.Range("B8").Value2
    wsCategories.Cells(classRow, 5).Value2 = wsView.Range("B9").Value2
    wsCategories.Cells(classRow, 6).Value2 = wsView.Range("B10").Value2

    WriteClassValues wsView, wsBase, 4, 5, className
    WriteClassValues wsView, wsAttr, 7, 8, className
    WriteClassValues wsView, wsAbilities, 11, 12, className

    csvDir = ThisWorkbook.Path
    ExportSheetCsv wsCategories, csvDir & "\categories.csv"
    ExportSheetCsv wsBase, csvDir & "\base_stats.csv"
    ExportSheetCsv wsAttr, csvDir & "\attributes.csv"
    ExportSheetCsv wsAbilities, csvDir & "\abilities.csv"
    ExportSheetCsv wsMeta, csvDir & "\meta.csv"

    repoRoot = CreateObject("Scripting.FileSystemObject").GetParentFolderName(CreateObject("Scripting.FileSystemObject").GetParentFolderName(csvDir))
    command = "powershell -NoProfile -ExecutionPolicy Bypass -File " & _
        Chr(34) & repoRoot & "\scripts\import_class_weights_from_csv.ps1" & Chr(34) & _
        " -ProjectRoot " & Chr(34) & repoRoot & Chr(34)

    Set shell = CreateObject("WScript.Shell")
    exitCode = shell.Run(command, 1, True)
    If exitCode <> 0 Then
        MsgBox "CSV export worked, but JSON import failed." & vbCrLf & vbCrLf & _
            "Close Excel, rebuild the workbook with .\openClassEditor.cmd, then try again." & vbCrLf & _
            "If it still fails, run this in the project root:" & vbCrLf & _
            "powershell -ExecutionPolicy Bypass -File scripts\import_class_weights_from_csv.ps1 -ProjectRoot .", vbExclamation
        Exit Sub
    End If
    LoadSelectedClassView
    Application.CalculateFull
    MsgBox "Applied class_view edits for '" & className & "' to CSV and JSON." & vbCrLf & _
        "CSV files were regenerated from JSON and the class view was reloaded." & vbCrLf & _
        "If the workbook was opened before the latest script changes, rebuild it with .\openClassEditor.cmd to refresh the embedded macro.", vbInformation
End Sub
'@

    try {
        $vbProject = $Workbook.VBProject
        $standardModule = $vbProject.VBComponents.Add(1)
        $standardModule.Name = "ClassViewModule"
        $standardModule.CodeModule.AddFromString($vbaCode)

        $workbookModule = $vbProject.VBComponents.Item("ThisWorkbook")
        $workbookCode = @'
Option Explicit

Private Sub Workbook_SheetChange(ByVal Sh As Object, ByVal Target As Range)
    On Error GoTo CleanExit
    If Sh Is Nothing Then Exit Sub
    If LCase$(Sh.Name) <> "class_view" Then Exit Sub
    If Target Is Nothing Then Exit Sub
    If Intersect(Target, Sh.Range("B3,H3,L3")) Is Nothing Then Exit Sub
    Application.EnableEvents = False
    LoadSelectedClassView
CleanExit:
    Application.EnableEvents = True
End Sub
'@
        $workbookModule.CodeModule.DeleteLines(1, $workbookModule.CodeModule.CountOfLines)
        $workbookModule.CodeModule.AddFromString($workbookCode)
    } catch {
        Write-Warning "Could not add VBA macro module. Enable 'Trust access to the VBA project object model' in Excel if you want the edit button."
    }
}

function Set-WorksheetData {
    param(
        [object]$Worksheet,
        [System.Collections.IList]$Rows
    )

    if ($Rows.Count -eq 0) {
        return
    }

    $headers = @($Rows[0].PSObject.Properties.Name)
    $rowCount = $Rows.Count + 1
    $columnCount = $headers.Count
    $values = New-Object 'object[,]' $rowCount, $columnCount

    for ($column = 0; $column -lt $columnCount; $column++) {
        $values[(0), $column] = $headers[$column]
    }

    for ($row = 0; $row -lt $Rows.Count; $row++) {
        for ($column = 0; $column -lt $columnCount; $column++) {
            $raw = $Rows[$row].($headers[$column])
            $parsed = $null
            if ($raw -is [string] -and [double]::TryParse($raw.Replace(",", "."), [System.Globalization.NumberStyles]::Float, [System.Globalization.CultureInfo]::InvariantCulture, [ref]$parsed)) {
                $values[($row + 1), $column] = $parsed
            } else {
                $values[($row + 1), $column] = $raw
            }
        }
    }

    $endCell = $Worksheet.Cells.Item($rowCount, $columnCount)
    $range = $Worksheet.Range("A1", $endCell)
    $range.Value2 = $values
}

function Format-Worksheet {
    param(
        [object]$Worksheet,
        [string]$TableName,
        [string]$SheetName
    )

    $usedRange = $Worksheet.UsedRange
    $rowCount = $usedRange.Rows.Count
    $columnCount = $usedRange.Columns.Count
    if ($rowCount -lt 1 -or $columnCount -lt 1) {
        return
    }

    $headerRange = $Worksheet.Range($Worksheet.Cells.Item(1, 1), $Worksheet.Cells.Item(1, $columnCount))
    $headerRange.Font.Bold = $true
    $headerRange.Interior.Color = 0xD9EAD3
    $headerRange.HorizontalAlignment = -4108

    $tableRange = $Worksheet.Range($Worksheet.Cells.Item(1, 1), $Worksheet.Cells.Item($rowCount, $columnCount))
    $listObject = $Worksheet.ListObjects.Add(1, $tableRange, $null, 1)
    $listObject.Name = $TableName
    $listObject.TableStyle = "TableStyleMedium2"

    $Worksheet.Activate() | Out-Null
    $Worksheet.Application.ActiveWindow.SplitRow = 1
    $Worksheet.Application.ActiveWindow.SplitColumn = 1
    $Worksheet.Application.ActiveWindow.FreezePanes = $true

    $usedRange.EntireColumn.AutoFit() | Out-Null

    for ($column = 1; $column -le $columnCount; $column++) {
        $currentWidth = $Worksheet.Columns.Item($column).ColumnWidth
        if ($currentWidth -gt 32) {
            $Worksheet.Columns.Item($column).ColumnWidth = 32
        }
    }

    if ($rowCount -ge 2 -and $columnCount -ge 2) {
        $bodyRange = $Worksheet.Range($Worksheet.Cells.Item(2, 2), $Worksheet.Cells.Item($rowCount, $columnCount))
        try {
            $null = $bodyRange.FormatConditions.Delete()
        } catch {
        }

        # Keep formatting robust across Excel versions: use number format and light alignment only.
        if ($SheetName -eq "base_stats" -or $SheetName -eq "attributes" -or $SheetName -eq "abilities") {
            $values = $bodyRange.Value2
            if ($values -is [array]) {
                for ($r = 1; $r -le $values.GetLength(0); $r++) {
                    for ($c = 1; $c -le $values.GetLength(1); $c++) {
                        $value = $values[$r, $c]
                        if ($value -is [double] -or $value -is [int] -or $value -is [decimal]) {
                            $values[$r, $c] = [double]$value * 10.0
                        }
                    }
                }
                $bodyRange.Value2 = $values
            }
            $bodyRange.NumberFormat = "0"
        } else {
            $bodyRange.NumberFormat = "0.0###"
        }
        $bodyRange.HorizontalAlignment = -4108
    }

    if ($SheetName -eq "categories") {
        $Worksheet.Columns.Item(1).ColumnWidth = 16
        $Worksheet.Columns.Item(2).ColumnWidth = 12
        $Worksheet.Columns.Item(3).ColumnWidth = 12
        $Worksheet.Columns.Item(4).ColumnWidth = 12

        if ($rowCount -ge 2) {
            $classRange = $Worksheet.Range($Worksheet.Cells.Item(2, 1), $Worksheet.Cells.Item($rowCount, 1))
            $classRange.Font.Bold = $true

            $baseRange = $Worksheet.Range($Worksheet.Cells.Item(2, 2), $Worksheet.Cells.Item($rowCount, 2))
            $attributeRange = $Worksheet.Range($Worksheet.Cells.Item(2, 3), $Worksheet.Cells.Item($rowCount, 3))
            $abilityRange = $Worksheet.Range($Worksheet.Cells.Item(2, 4), $Worksheet.Cells.Item($rowCount, 4))

            $baseRange.NumberFormat = "0.0\%"
            $attributeRange.NumberFormat = "0.0\%"
            $abilityRange.NumberFormat = "0.0\%"

            $baseRange.Interior.Color = 0xD9EAD3
            $attributeRange.Interior.Color = 0xD0E0E3
            $abilityRange.Interior.Color = 0xF4CCCC
        }
    }

    if ($SheetName -eq "meta") {
        $Worksheet.Columns.Item(1).ColumnWidth = 28
        $Worksheet.Columns.Item(2).ColumnWidth = 14
    }

    if ($SheetName -eq "class_risk") {
        $Worksheet.Columns.Item(1).ColumnWidth = 16
        for ($column = 2; $column -le [Math]::Min($columnCount, 5); $column++) {
            $Worksheet.Columns.Item($column).ColumnWidth = 24
        }
        if ($rowCount -ge 2 -and $columnCount -ge 2) {
            $riskRange = $Worksheet.Range($Worksheet.Cells.Item(2, 2), $Worksheet.Cells.Item($rowCount, $columnCount))
            $riskRange.NumberFormat = "0.0%"
        }
    }
}

function Add-CsvSheet {
    param(
        [object]$Workbook,
        [string]$SheetName,
        [string]$CsvPath,
        [int]$InsertIndex
    )

    $rows = Import-CsvAutoDelimiter -Path $CsvPath
    $worksheet = $Workbook.Worksheets.Add()
    $worksheet.Move($Workbook.Worksheets.Item($InsertIndex))
    $worksheet.Name = $SheetName
    Set-WorksheetData -Worksheet $worksheet -Rows $rows
    $safeTableName = ("tbl_" + ($SheetName -replace "[^A-Za-z0-9_]", "_"))
    Format-Worksheet -Worksheet $worksheet -TableName $safeTableName -SheetName $SheetName
    return $worksheet
}

$resolvedCsvDir = Resolve-Path $CsvDir
$resolvedOutputDir = Split-Path -Parent $OutputPath
if (-not (Test-Path $resolvedOutputDir)) {
    New-Item -ItemType Directory -Path $resolvedOutputDir | Out-Null
}
$resolvedOutputPath = (Resolve-Path $resolvedOutputDir).Path + "\" + (Split-Path -Leaf $OutputPath)

$sheetOrder = @(
    @{ Name = "categories"; File = "categories.csv" },
    @{ Name = "base_stats"; File = "base_stats.csv" },
    @{ Name = "attributes"; File = "attributes.csv" },
    @{ Name = "abilities"; File = "abilities.csv" },
    @{ Name = "class_risk"; File = "class_risk.csv" },
    @{ Name = "meta"; File = "meta.csv" }
)

$excel = $null
$workbook = $null
$defaultSheet = $null

try {
    $excel = New-Object -ComObject Excel.Application
    $excel.Visible = [bool]$Open
    $excel.DisplayAlerts = $false

    $workbook = $excel.Workbooks.Add()
    $defaultSheet = $workbook.Worksheets.Item(1)

    $insertIndex = 1
    foreach ($sheet in $sheetOrder) {
        $csvPath = Join-Path $resolvedCsvDir $sheet.File
        if (-not (Test-Path $csvPath)) {
            throw "Missing CSV file: $csvPath"
        }
        Add-CsvSheet -Workbook $workbook -SheetName $sheet.Name -CsvPath $csvPath -InsertIndex $insertIndex | Out-Null
        $insertIndex++
    }

    Add-ClassViewSheet -Workbook $workbook | Out-Null
    Add-VbaMacroModule -Workbook $workbook

    $defaultSheet.Delete()
    $workbook.Worksheets.Item("class_view").Move($workbook.Worksheets.Item(1))
    $workbook.Worksheets.Item(1).Activate() | Out-Null

    $workbook.SaveAs($resolvedOutputPath, 52)

    if (-not $Open) {
        $workbook.Close($true)
        $excel.Quit()
    }

    Write-Host "Created workbook: $resolvedOutputPath"
    if ($Open) {
        Write-Host "Workbook left open in Excel."
    }
}
finally {
    if (-not $Open) {
        Release-ComObject $defaultSheet
        Release-ComObject $workbook
        Release-ComObject $excel
        [GC]::Collect()
        [GC]::WaitForPendingFinalizers()
    }
}
