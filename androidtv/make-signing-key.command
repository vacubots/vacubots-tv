#!/bin/bash
# Makes the private signing key for the VacuBots TV app and walks you through saving it in GitHub.
# Double-click this file (right-click, Open the first time). Needs Java (keytool); see the README in the repository.
# The password is never written to a file. The key file is saved as ~/vacubots-release.keystore: keep a copy somewhere safe.
KS="$HOME/vacubots-release.keystore"
URL="https://github.com/vacubots/vacubots-tv/settings/secrets/actions"
say() { printf '\n%s\n' "$1"; }
wait_enter() { printf '%s' "Press Enter when done... "; read -r _; }

command -v keytool >/dev/null 2>&1 && keytool -help >/dev/null 2>&1 || { say "Java is not installed. Install Temurin 21 (JDK, .pkg) from adoptium.net, then double-click this again."; exit 1; }
[ -e "$KS" ] && { say "A key file already exists at $KS. Move it to the trash first if you really want a new one, then run this again."; exit 1; }

clear
say "STEP 1 of 3: THE PASSWORD"
P=$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-28)
say "This is a PASSWORD for the key. It is NOT the key itself. It is shown only this once:"
printf '\n    %s\n\n' "$P"
say "Copy it into your password manager NOW, and check that it saved."
printf '%s' "(To use your own password instead, type it now and press Enter; otherwise just press Enter.) "
read -r -s OWN; echo
[ -n "$OWN" ] && P="$OWN"
while true; do
  printf '%s' "When it is saved, type the word  saved  and press Enter: "
  read -r ANS
  [ "$(printf '%s' "$ANS" | tr 'A-Z' 'a-z')" = "saved" ] && break
done

clear
say "STEP 2 of 3: MAKING THE KEY FILE"
keytool -genkeypair -keystore "$KS" -alias vacubots -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=VacuBots" -storepass "$P" -keypass "$P" >/dev/null 2>&1
keytool -list -keystore "$KS" -storepass "$P" 2>/dev/null | grep -q "vacubots," || { say "The key could not be made or checked. Nothing was saved to GitHub. Tell Claude."; exit 1; }
say "Key made and checked. It is the file $KS (a copy of it goes to GitHub in a moment; you do not paste it by hand)."
wait_enter

clear
say "STEP 3 of 3: SAVE FOUR SECRETS IN GITHUB"
say "A GitHub page is about to open. Each secret has a NAME and a VALUE. I show the NAME first and wait; only after you press Enter do I copy the VALUE for you to paste."
open "$URL"; wait_enter

# secret NUMBER NAME "what the value is" VALUE
secret() {
  clear
  say "SECRET $1 of 4"
  printf '\n    NAME:   %s\n    VALUE:  %s\n\n' "$2" "$3"
  say "On the GitHub page: click \"New repository secret\" (or the pencil next to $2 if it is already listed)."
  say "Type this name into the Name box:  $2"
  printf '%s' "Press Enter when the name is typed, and I will copy the value... "; read -r _
  printf '%s' "$4" | pbcopy
  say "Copied: $3."
  say "Now click in the Secret box, paste (Command-V), and click \"Add secret\" (or \"Update secret\")."
  wait_enter
  printf '' | pbcopy
}
secret 1 VB_KEYSTORE_B64 "the key file (a long block of letters)" "$(base64 -i "$KS")"
secret 2 VB_KEYSTORE_PASSWORD "the password from step 1" "$P"
secret 3 VB_KEY_ALIAS "the word vacubots" "vacubots"
secret 4 VB_KEY_PASSWORD "the password from step 1 again" "$P"

printf '' | pbcopy; unset P OWN
clear
say "DONE. The clipboard is cleared and you can close this window."
say "Keep a copy of $KS (a USB stick or a second backup folder) and your password somewhere safe: if either is lost, the TV app can never be updated in place again."
say "Now tell Claude the four secrets are saved."
