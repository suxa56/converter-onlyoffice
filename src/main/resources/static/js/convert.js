const docInputExt = []
const spreadsheetInputExt = []
const presentationInputExt = []
const docOutputExt = []
const spreadsheetOutputExt = []
const presentationOutputExt = []

const DOCUMENT = 'docx'
const SPREADSHEET = 'xlsx'
const PRESENTATION = 'pptx'

const input = document.querySelector('#file')
const errorMessage = document.querySelector('#error_message')
const outputExt = document.querySelector('#ext')
const downloadLink = document.querySelector('#download')

window.onload = config()
document.onload = linkInteract()

input.addEventListener("input", function () {
    let filename = input.value
    clearOptions()
    let ext = filename.substring(filename.lastIndexOf("."))
    let inputType = defineInputType(ext)
    console.log('input type = ' + inputType)
    if (inputType !== '') {
        errorMessage.type = 'hidden'
        if (inputType === DOCUMENT) {
            docOutputExt[0].forEach(el => {
                console.log('docs fill')
                createOptions(el, ext)
            })
        } else if (inputType === SPREADSHEET) {
            spreadsheetOutputExt[0].forEach(el => {
                console.log('spread fill')
                createOptions(el, ext)
            })
        } else if (inputType === PRESENTATION) {
            presentationOutputExt[0].forEach(el => {
                console.log('present fill')
                createOptions(el, ext)
            })
        }
    } else {
        errorMessage.type = 'text'
    }
})

async function config () {
    await fetch("http://192.168.100.15:9090/config", {
        method: 'POST',
        headers: {
            "Content-type": "application/json; charset=UTF-8"
        },
    }).then((response) => response.json())
        .then((json) => {
            JSON.parse(JSON.stringify(json), (key, value) => {
                if (key === "DocInputExtList") {
                    docInputExt.push(value.toString().split(','))
                }
                if (key === "SpreadsheetInputExtList") {
                    spreadsheetInputExt.push(value.toString().split(','))
                }
                if (key === "PresentationInputExtList") {
                    presentationInputExt.push(value.toString().split(','))
                }
                if (key === "DocOutputExtList") {
                    docOutputExt.push(value.toString().split(','))
                }
                if (key === "SpreadsheetOutputExtList") {
                    spreadsheetOutputExt.push(value.toString().split(','))
                }
                if (key === "PresentationOutputExtList") {
                    presentationOutputExt.push(value.toString().split(','))
                }
            })
        })
}

function linkInteract () {
    if (downloadLink.href !== window.location.href) {
        downloadLink.click()
    }
    document.body.removeChild(downloadLink)
}


function defineInputType(extension) {
    if (docInputExt[0].includes(extension)) {
        return DOCUMENT
    }
    if (spreadsheetInputExt[0].includes(extension)) {
        return SPREADSHEET
    }
    if (presentationInputExt[0].includes(extension)) {
        return PRESENTATION
    }
    return ''
}

function createOptions(element, extension) {
    if (element !== extension) {
        const option = document.createElement("option")
        option.textContent = element
        option.value = element
        outputExt.appendChild(option)
    }
}

function clearOptions() {
    while (outputExt.firstChild.value !== outputExt.lastChild.value) {
        console.log('cleared ' + outputExt.lastChild.value)
        outputExt.removeChild(outputExt.lastChild)
    }
}