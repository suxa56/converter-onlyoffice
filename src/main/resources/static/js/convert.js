const docsIF = new Array()
const input = document.querySelector('#conv_input')
const errorMessage = document.querySelector('#error_message')

window.onload = async function () {
    await fetch("http://192.168.100.15:9090/config", {
        method: 'POST',
        headers: {
            "Content-type": "application/json; charset=UTF-8"
        },
    }).then((response) => response.json())
        .then((json) => {
            JSON.parse(JSON.stringify(json), (key, value) => {
                docsIF.push(value.toString().split(','))
            })
        })
}

input.addEventListener("input", function () {
    let filename = input.value
    let ext = filename.substring(filename.lastIndexOf("."))
    if (!docsIF[0].includes(ext)) {
        errorMessage.type = 'text'
    } else {
        errorMessage.type = 'hidden'
    }
})