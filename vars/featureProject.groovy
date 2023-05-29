
@NonCPS
def call(text) {
    def matcher = text =~ 'feature-(.+)-.+'
    matcher ? matcher[0][1] : null
}
