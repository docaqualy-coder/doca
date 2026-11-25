# Agente GestorCorp para Android

Aplicativo Android que aplica no aparelho a política definida no painel: recebe as
regras pela sincronização e intercepta os aplicativos restritos, mostrando a tela de
bloqueio com o motivo, a liberação por senha e o pedido de liberação.

## Como compilar

1. Abra a pasta `android/` no **Android Studio** (Giraffe ou mais novo).
2. Deixe o Android Studio baixar o Gradle e as dependências.
3. Conecte o celular e rode `Run`, ou gere o APK em
   **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Instale o APK no celular (`adb install app/build/outputs/apk/debug/app-debug.apk`).

O endereço do painel fica em `app/src/main/java/br/com/gestorcorp/agente/Api.kt`
(constante `BASE`).

## Como colocar o aparelho em conformidade

1. Abra o agente e digite o **código de vínculo** gerado no painel
   (Celulares → Vincular celular). O agente guarda a credencial do aparelho.
2. Toque em **Ativar acessibilidade** e autorize o serviço GestorCorp.
3. Toque em **Ativar administrador do aparelho** e confirme.
4. Toque em **Sincronizar**. O painel passa a mostrar a permissão autorizada.

A partir daí, ao abrir um aplicativo marcado como restrito na política, o agente
volta para a tela inicial e mostra a tela de bloqueio.

## Como funciona

| Peça | Papel |
| --- | --- |
| `Api.kt` | Fala com `/api/device/enroll`, `/api/device/sync` e `/api/device/report` |
| `SyncWorker.kt` | Sincroniza a política a cada 15 minutos e informa as permissões |
| `AgenteServico.kt` | Serviço de acessibilidade: identifica o app aberto e bloqueia |
| `BloqueioActivity.kt` | Tela de bloqueio: motivo, senha da política e liberação temporária |
| `Senha.kt` | Valida a senha offline (PBKDF2-SHA256, mesmos parâmetros do painel) |
| `Preferencias.kt` | Guarda credencial, política recebida e liberações temporárias |

O bloqueio usa o serviço de acessibilidade (não desenha sobreposição de tela), então
é estável entre versões do Android. A senha nunca sai do aparelho: o hash vem na
política e a validação é feita localmente.

## Gerar o APK pela web (sem instalar nada)

O projeto inclui `.github/workflows/build-apk.yml`, que compila o APK no GitHub:

1. Crie um repositório novo em github.com (gratuito).
2. Envie **o conteúdo** desta pasta para o repositório (`Add file → Upload files`).
3. Aba **Actions → Gerar APK → Run workflow**.
4. Baixe o APK em **Artifacts → gestorcorp-agente-apk**.

## Limites conhecidos

- O Android exige que o funcionário autorize acessibilidade e administrador do
  aparelho; sem isso o agente não consegue interceptar os aplicativos.
- Bloqueio de desinstalação e de instalações desconhecidas depende do administrador
  do aparelho e varia conforme o fabricante.
- Estatísticas de uso (UsageStatsManager) não estão incluídas nesta versão.
