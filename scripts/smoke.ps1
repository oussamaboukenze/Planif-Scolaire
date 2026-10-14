param(
    [string]$Referentiel = 'http://localhost:8081',
    [string]$Planning = 'http://localhost:8082',
    [string]$Presences = 'http://localhost:8083'
)
$ErrorActionPreference = 'Stop'
function Post-Json($Uri, $Body) {
    Invoke-RestMethod -Uri $Uri -Method Post -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json)))
}
function Wait-Result([scriptblock]$Read, [scriptblock]$Condition) {
    $deadline = (Get-Date).AddSeconds(45)
    do {
        try {
            $value = & $Read
            if (& $Condition $value) { return $value }
        } catch {
            if ($null -eq $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 404) { throw }
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw 'Délai dépassé en attendant la propagation Kafka.'
}
function Assert-Conflict([scriptblock]$Action) {
    try { & $Action | Out-Null } catch {
        if ($null -ne $_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 409) { return }
        throw
    }
    throw 'Un refus HTTP 409 était attendu.'
}
$suffix = [guid]::NewGuid().ToString('N').Substring(0,8)
$classe = Post-Json "$Referentiel/api/classes" @{nom="6A-$suffix";niveau='6e';effectifMax=30}
$enseignant = Post-Json "$Referentiel/api/enseignants" @{nom="Enseignant-$suffix";matiere='Maths';chargeMaxHeures=20}
$body = @{classeId=$classe.id;enseignantId=$enseignant.id;matiere='Maths';dateHeure='2026-11-02T09:00:00';salle="A-$suffix"}
$cours = Post-Json "$Planning/api/cours" $body
Assert-Conflict { Post-Json "$Planning/api/cours" $body }
$appel = Wait-Result { Invoke-RestMethod "$Presences/api/appels/cours/$($cours.id)" } { param($a) $a.statut -eq 'OUVERT' }
$absence = @{etudiantNom='Sara Ali';motif='Maladie';justifiee=$true}
Assert-Conflict { Post-Json "$Presences/api/appels/$($appel.id)/absences" $absence }
Invoke-RestMethod "$Planning/api/cours/$($cours.id)/statut" -Method Patch -ContentType 'application/json' -Body '{"statut":"REALISE"}' | Out-Null
Post-Json "$Presences/api/appels/$($appel.id)/absences" $absence | Out-Null
Assert-Conflict { Post-Json "$Presences/api/appels/$($appel.id)/absences" $absence }
$suivi = Wait-Result { Invoke-RestMethod "$Planning/api/cours/$($cours.id)" } { param($c) $c.nbAbsencesSignalees -eq 1 }
$body.dateHeure = '2026-11-02T10:00:00'
$annule = Post-Json "$Planning/api/cours" $body
Invoke-RestMethod "$Planning/api/cours/$($annule.id)/statut" -Method Patch -ContentType 'application/json' -Body '{"statut":"ANNULE"}' | Out-Null
$cloture = Wait-Result { Invoke-RestMethod "$Presences/api/appels/cours/$($annule.id)" } { param($a) $a.statut -eq 'CLOTURE' }
Write-Output "OK : cours $($cours.id), appel $($appel.id), compteur $($suivi.nbAbsencesSignalees), annulation $($annule.id)."
