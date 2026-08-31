{{/*
Common labels applied to every object this chart renders.
*/}}
{{- define "agrawalpulse.labels" -}}
app.kubernetes.io/part-of: agrawalpulse
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/*
Selector labels for one service - must be stable across releases (never include version/tag),
since these are what Deployment.spec.selector and Service.spec.selector match on.
*/}}
{{- define "agrawalpulse.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/part-of: agrawalpulse
{{- end -}}

{{/*
Resolve a service's resource requests/limits from its `tier`, falling back to `standard`
if the service entry omits one. Usage: {{ include "agrawalpulse.resources" (dict "svc" $svc "root" $) }}
*/}}
{{- define "agrawalpulse.resources" -}}
{{- $tier := .svc.tier | default "standard" -}}
{{- $res := index .root.Values.global.resourceTiers $tier -}}
{{ toYaml $res }}
{{- end -}}
