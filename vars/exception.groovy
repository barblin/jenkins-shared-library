def handle(e) {
    echo 'Build failed'
    step([$class                  : 'Mailer',
          notifyEveryUnstableBuild: true,
          recipients              : [emailextrecipients(
                  [[$class: 'CulpritsRecipientProvider'],
                   [$class: 'RequesterRecipientProvider']])].join(' ')])
}