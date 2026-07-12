{{- define "shift-swap-service.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "shift-swap-service.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name -}}
{{- end -}}

{{- define "shift-swap-service.labels" -}}
app.kubernetes.io/name: {{ include "shift-swap-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: Helm
{{- end -}}

{{- define "shift-swap-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "shift-swap-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
