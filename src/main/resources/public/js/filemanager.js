// Jquery Serialization Plug-In
// https://tdanemar.wordpress.com/2010/08/24/jquery-serialize-method-and-checkboxes/
$.fn.serialize = function (options) {
    return $.param(this.serializeArray(options));
};

$.fn.serializeArray = function (options) {
    const o = $.extend({
        checkboxesAsBools: false
    }, options || {});

    const rselectTextarea = /select|textarea/i;
    const rinput = /text|hidden|password|search/i;

    return this.map(function () {
        return this.elements ? $.makeArray(this.elements) : this;
    })
        .filter(function () {
            return this.name && !this.disabled &&
                (this.checked
                    || (o.checkboxesAsBools && this.type === 'checkbox')
                    || rselectTextarea.test(this.nodeName)
                    || rinput.test(this.type));
        })
        .map(function (i, elem) {
            const val = $(this).val();
            return val == null ?
                null :
                $.isArray(val) ?
                    $.map(val, function (val, i) {
                        return {name: elem.name, value: val};
                    }) :
                    {
                        name: elem.name,
                        value: (o.checkboxesAsBools && this.type === 'checkbox') ? //moar ternaries!
                            (this.checked ? 'true' : 'false') :
                            val
                    };
        }).get();
};


// https://stackoverflow.com/a/39845980/11324248
// https://www.sitepoint.com/building-custom-right-click-context-menu-javascript/
// https://codepen.io/SitePoint/pen/MYLoWY
// Adapted to work with the fomantic-ui framework


//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//
// H E L P E R    F U N C T I O N S
//
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

/**
 * Function to check if we clicked inside an element with a particular class
 * name.
 *
 * @param {Object} e The event
 * @param {String} className The class name to check against
 * @return {Boolean}
 */
function clickInsideElement(e, className) {
    let el = e.srcElement || e.target;

    if (el.classList.contains(className)) {
        return el;
    } else {
        while (el = el.parentNode) {
            if (el.classList && el.classList.contains(className)) {
                return el;
            }
        }
    }

    return false;
}

/**
 * Get's exact position of event.
 *
 * @param {Object} e The event passed in
 * @return {Object} Returns the x and y position
 */
function getPosition(e) {
    let posX = 0;
    let posY = 0;

    if (!e) var e = window.event;

    if (e.pageX || e.pageY) {
        posX = e.pageX;
        posY = e.pageY;
    } else if (e.clientX || e.clientY) {
        posX = e.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
        posY = e.clientY + document.body.scrollTop + document.documentElement.scrollTop;
    }

    return {
        x: posX,
        y: posY
    }
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//
// C O R E    F U N C T I O N S
//
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

/**
 * Variables.
 */
const contextMenuClassName = "context-menu";
const contextMenuLinkClassName = "context-item";
const contextMenuActive = "context-menu--active";

const taskItemClassName = "context-clickable";
let rowInContext;
const taskItemActiveClass = "active";

let clickCoords;
let clickCoordsX;
let clickCoordsY;

const menu = document.querySelector("#context-menu");
let menuState = 0;
let menuWidth;
let menuHeight;
let menuPosition;
let menuPositionX;
let menuPositionY;

let windowWidth;
let windowHeight;


/*
var table = $('#fileTable').DataTable({
    paging: false,
    ordering: false,
    searching: false,
    info: false,
    select: true
});*/

let ds = null;

// custom drag drop handling

var customDragging = false;
var startElement = null;

document.addEventListener("dragstart", function (e) {
    if ($(e.target).hasClass("drag-startable")) {
        startElement = $(e.target).closest("tr").attr("data-id");
        customDragging = true;
        var elem = document.createElement("div");
        elem.id = "drag-ghost";
        elem.textNode = "Dragging";
        elem.classList.add("ui")
        elem.classList.add("inverted")
        elem.classList.add("segment")
        elem.style.position = "absolute";
        elem.style.top = "-1000px";

        elem.appendChild(document.createTextNode("Moving " + getSelectedRows().length + " item" + (getSelectedRows().length > 1 ? "s" : "")));

        document.body.appendChild(elem);
        e.dataTransfer.setDragImage(elem, -20, 0);
    }
}, false);

document.addEventListener("dragover", function (e) {
    if (customDragging) {
        const target = $(e.target)
        const closestTr = target.closest("tr")

        $("tr").removeClass("ds-drop-target");
        $(".section").removeClass("ds-drop-target");

        if (closestTr.length > 0 && closestTr.attr("data-folder") === "true" && closestTr.attr("data-id") !== startElement) {
            console.log(startElement)
            closestTr.addClass("ds-drop-target")
            e.preventDefault();
        } else if (target.hasClass("section")) {
            target.addClass("ds-drop-target")
            e.preventDefault();
        } else {

        }
    }
});

document.addEventListener("drop", function (e) {
    if (customDragging) {
        var targetId = "";
        const target = $(e.target)
        const closestTr = target.closest("tr")

        if (closestTr.length > 0 && closestTr.attr("data-folder") === "true") {
            targetId = closestTr.attr("data-id");
        } else {
            if (target.hasClass("section")) {
                targetId = target.attr("data-id");
            }
        }

        if (targetId !== "") {
            const filesToMove = getSelectedRows().map(x => $(x).attr("data-id"));

            if (filesToMove.indexOf(targetId) !== -1) {
                $('body')
                    .toast({
                        class: 'error',
                        message: "You can't move a folder into itself!"
                    })
                ;
                return;
            }
            const ids = filesToMove.join(",");
            const data = new FormData();
            data.append("files", ids);

            $.ajax({
                url: "/api/v1/files/" + targetId + "/setAsChildren",
                type: "POST",
                enctype: "multipart/form-data",
                data: data,
                processData: false,
                contentType: false,

                success: function (result) {
                    htmx.trigger("#refreshButton", "refreshTable");
                },
                error: function (error) {
                    $('body')
                        .toast({
                            class: 'error',
                            message: 'Could not delete file: ' + error
                        })
                    ;
                }
            });
            e.preventDefault();
        }
    }
});

// Let's remove the created ghost elem on dragend
document.addEventListener("dragend", function (e) {
    if (customDragging) {
        $("tr").removeClass("ds-drop-target");
        $(".section").removeClass("ds-drop-target");
        var ghost = document.getElementById("drag-ghost");
        if (ghost.parentNode) {
            ghost.parentNode.removeChild(ghost);
        }
    }
    customDragging = false;
}, false);


function initializeDragSelect() {
    ds = new DragSelect({
        selectables: document.getElementsByClassName('context-clickable'),
        area: document.getElementById('fileTableBody'),
        draggability: false, // implement this instead: https://interactjs.io/docs/
        keyboardDrag: false,
        //selectedClass: 'selected'
    });
    // fired once the user releases the mouse. (items) = selected nodes:
    /* ds.subscribe('callback', ({ items, event }) => {
         table.rows().deselect();
         table.rows('.ds-selected').select();
         console.log(items)
     });*/
    ds.subscribe('predragstart', ({event}) => {
        if (event.type != "mousedown") {
            ds.stop()
        }
        // only show custom mdoal when we are dragging a link; TODO: check if this is a custom element, or other things will cause this too.
        if (event.target instanceof HTMLAnchorElement) {
            const row = event.target.closest(".context-clickable");
            if (!isSelected(row)) {
                clearSelection()
                selectRow(row);
            }
            ds.break();
        }
    });
    ds.subscribe('dragstart', ({event}) => {
        if (event === "mousedown") {
            // console.log(event)
        }
        toggleMenuOff();
    })
    ds.subscribe('elementselect', ({item, items}) => {
        // console.log(item)
        // $(item).find(".checkbox")[0].checked = true;

        selectRow(item);
        if (items.length == $(".context-clickable").length - 1) {
            $("#toggleAllSelection")[0].checked = true;
        }
    });
    ds.subscribe('elementunselect', ({item, items}) => {
        /// $(item).find(".checkbox")[0].checked = false;

        deselectRow(item);

        $("#toggleAllSelection")[0].checked = false;
        // console.log(items)
    });
}

/*
        $('#fileTable').selectable({
            classes: {
                "ui-selected": "selected"
            },
            filter: 'tr',
            stop: function () {
                console.log(this);
                $(".ui-selected", this).each(function () {
                    var index = $("#fileTable tr").index(this) - 1;
                    $('#fileTable tbody tr:eq(' + index + ')').toggleClass('active');
                    $('#fileTable tbody tr:eq(' + index + ')').toggleClass('ui-selected');
                });
            }
        });
*/
/**
 * Initialise our application's code.
 */
function init() {
    initializeDragSelect()
    contextListener();
    clickListener();
    keyupListener();
    resizeListener();
    console.log("registered context menu listeners etc.")
}


function isSelected(row) {
    return ds.getSelection().indexOf(row) !== -1;
}

function getSelectedRows() {
    return ds.getSelection()
}

function getAllRows() {
    return ds.getSelectables();
}

function toggleSelection() {
    if ($("#toggleAllSelection")[0].checked) {
        selectAll();
    } else {
        clearSelection();
    }
}

function selectAll() {
    $(".context-clickable").each(function (_, item) {
        selectRow(item)
    });
    //ds.addSelection($(".context-clickable"))
}

function clearSelection() {
    ds.getSelection().forEach(function (item) {
        deselectRow(item)
    });
}

function selectRow(row) {
    ds.addSelection(row)
    $(row).find(":checkbox")[0].checked = true;
}

function deselectRow(row) {
    ds.removeSelection(row)
    $(row).find(":checkbox")[0].checked = false;
}

/**
 * Listens for contextmenu events.
 */
function contextListener() {
    document.addEventListener("contextmenu", function (e) {
        // another item is clicked
        if (rowInContext && getSelectedRows().length == 1) {
            clearSelection();
            //table.rows(rowInContext).deselect();
            // rowInContext.classList.remove(taskItemActiveClass)
        }
        rowInContext = clickInsideElement(e, taskItemClassName);

        // row sensitive context menu button visibility
        if (ds.getSelection().length <= 1 && $(rowInContext).attr("data-mime") && $(rowInContext).attr("data-mime").split("/")[0] === "image") {
            $("#contextMenuPreviewButton").show()
        } else {
            $("#contextMenuPreviewButton").hide()
        }

        if (rowInContext) {
            e.preventDefault();
            toggleMenuOn();
            positionMenu(e);
        } else {
            rowInContext = null;
            toggleMenuOff();
        }
    });
}

/**
 * Listens for click events.
 */
function clickListener() {
    const fileTable = document.querySelector('#fileTable')

    document.addEventListener("click", function (e) {
        if (event.target.type === "checkbox" && event.target.id != "toggleAllSelection" && event.composedPath().includes(fileTable)) {
            const row = event.target.closest(".context-clickable");
            if (event.target.checked) {
                // not using addSelection because that uses invalid jquery on mobile
                selectRow(row)
                // ds.addSelection(row)
            } else {
                // not using deselectRow because that uses invalid jquery on mobile
                deselectRow(row)
                //  ds.removeSelection(row)
            }
        }

        const clickeElIsLink = clickInsideElement(e, contextMenuLinkClassName);

        if (clickeElIsLink) {
            e.preventDefault();
            menuItemListener(clickeElIsLink);
        } else {
            // this closes the menu when not clicking on the menu
            // this closes the menu when the dropdown is clicked, so I disabled it (without really understanding the code)
            const button = e.which || e.button;
            if (button === 1) {
                toggleMenuOff();
            }
        }

        // clear selection when clicking outside of the table
        const withinBoundaries = e.composedPath().includes(fileTable)
        if (!withinBoundaries && !e.target.classList.contains("context-item-dropdown")) {
            ds.clearSelection();
        }
    }, false);
}

/**
 * Listens for keyup events.
 */
function keyupListener() {
    window.onkeyup = function (e) {
        if (e.keyCode === 27) {
            toggleMenuOff();
        }
    }
}

/**
 * Window resize event listener
 */
function resizeListener() {
    window.onresize = function (e) {
        toggleMenuOff();
    };
}

/**
 * Turns the custom context menu on.
 */
function toggleMenuOn() {
    if (getSelectedRows().length <= 1) {
        ds.clearSelection();
        ds.addSelection(rowInContext);
        //table.rows().deselect();
        //table.rows(rowInContext).select();
        //rowInContext.classList.add(taskItemActiveClass)
    }
    if (menuState !== 1) {
        menuState = 1;
        menu.classList.add(contextMenuActive);
    }
}

/**
 * Turns the custom context menu off.
 */
function toggleMenuOff() {
    if (menuState !== 0) {
        menuState = 0;
        menu.classList.remove(contextMenuActive);
        menu.style.top = "";
        menu.style.left = "";

        // ds.clearSelection();

        // table.rows().deselect();
        /*if (getSelectedRows().length == 1) {
            //rowInContext.classList.remove(taskItemActiveClass)
            table.rows(rowInContext).deselect();
        }*/
    }
}

/**
 * Positions the menu properly.
 *
 * @param {Object} e The event
 */
function positionMenu(e) {
    clickCoords = getPosition(e);
    console.log(clickCoords)
    clickCoordsX = clickCoords.x;
    clickCoordsY = clickCoords.y;

    menuWidth = menu.offsetWidth + 4;
    menuHeight = menu.offsetHeight + 4;

    menu.style.left = clickCoordsX + "px";
    menu.style.top = clickCoordsY + "px";
}

function convertRowsToXHRData(rows) {
    const ids = rows.map(function (row) {
        return row.getAttribute("data-id");
    }).join(",");
    const data = new FormData();
    data.append("files", ids);
    return data;
}


function startFileDownload(rows) {
    if (rows.length === 1 && !rows[0].getAttribute("data-folder")) {
        window.location.href = "/api/v1/files/" + rows[0].getAttribute("data-id") + "?download";
    } else {

        let fileIdList = convertRowsToXHRData(rows)
        showFileDownloadModal(rows.length, -1);
        // https://stackoverflow.com/a/29556434
        const xhr = new XMLHttpRequest();
        xhr.open("PUT", "/api/v1/files", true);
        xhr.setRequestHeader("x-client", "web/api");
        xhr.responseType = 'blob';
        // set multipart data
        xhr.onprogress = function (e) {
            // console.log(e.loaded + " / " + e.total);
            if (e.lengthComputable) {
                const percentComplete = (e.loaded / e.total) * 100;
                showFileDownloadModal(rows.length, Math.round(percentComplete));
            }
        };
        xhr.onload = function (e) {
            if (this.status === 200) {
                const filename = e.target.getResponseHeader("Content-Disposition").split(" ")[1].split("=")[1];
                const blob = new Blob([this.response], {type: 'application/zip'});
                const downloadUrl = URL.createObjectURL(blob);
                const a = document.createElement("a");
                a.href = downloadUrl;
                a.download = filename;
                document.body.appendChild(a);
                a.click();
            } else {
                const blob = new Blob([this.response], {type: 'application/zip'});
                const fileReader = new FileReader();
                fileReader.onload = (e) => {
                    $('body')
                        .toast({
                            class: 'red',
                            message: JSON.parse(e.target.result)[0],
                            position: 'bottom right',
                            displayTime: 5000,
                            showProgress: 'bottom',
                        })
                    ;
                };
                fileReader.readAsText(blob)

            }
            hideFileDownloadModal();
        };
        xhr.onabort = function (e) {
            $('body')
                .toast({
                    class: 'error',
                    message: 'Could not download files as zip'
                })
            ;
            hideFileDownloadModal();
        }
        xhr.send(fileIdList);
    }
}

/**
 * Dummy action function that logs an action when a menu item link is clicked
 *
 * @param {HTMLElement} link The link that was clicked
 */
function menuItemListener(link) {
    // the dropdown menu should not close the whole menu when clicked, and itself should not be a clickable link, but yet it should not be considered outside the menu (which would lead it to close)
    if (!link.classList.contains("context-item-dropdown")) {
        const rows = getSelectedRows();
        if (rows.length > 0) {
            switch (link.getAttribute("data-action")) {
                case "delete":
                    if (rows.length >= 2 || (rows.length === 1 && rows[0].getAttribute("data-folder") === "true")) {
                        showFileDeletionLoadingModal(rows.length);
                    }
                    $.ajax({
                        url: "/api/v1/files",
                        type: "DELETE",
                        enctype: "multipart/form-data",
                        data: convertRowsToXHRData(rows),
                        processData: false,
                        contentType: false,

                        success: function (result) {
                            // console.log(result);
                            // rows.remove();
                            // ds.removeSelectables(rowsToDelete)
                            // updateVisibilityOfEmptyFolder();
                            hideFileDeletionLoadingModal();
                            htmx.trigger("#refreshButton", "refreshTable");
                        },
                        error: function (error) {
                            // show fomantic toast
                            hideFileDeletionLoadingModal();
                            $('body')
                                .toast({
                                    class: 'error',
                                    message: 'Could not delete file: ' + error
                                })
                            ;
                        }
                    });
                    break;
                case "download":
                    startFileDownload(rows);
                    break;
                case "shares":
                    showShareListModal(rows[0]);
                    break;
                case "new-folder":
                    //TODO: fix this
                    promptFolderCreation(document.getElementById("parentId").value);
                    break;
                case "edit":
                    promptFileEdit(rows[0]);
                    break;
                case "preview":
                    showImagePreviewModal("/api/v1/files/" + rows[0].getAttribute("data-id"));
                    break;
            }
        }
        getSelectedRows().forEach(function (row) {
                switch (link.getAttribute("data-action")) {
                    // case "download":
                    //     window.location.href = "/api/v1/files/" + row.getAttribute("data-id") + "?download";
                    //     break;
                    /*case "delete":
                        const fileId = row.getAttribute("data-id");
                        $.ajax({
                            url: "/api/v1/files/" + fileId,
                            type: "DELETE",
                            success: function (result) {
                                // console.log(result);
                                ds.removeSelectables(row)
                                row.remove();
                                updateVisibilityOfEmptyFolder();
                            },
                            error: function (error) {
                                // show fomantic toast
                                $('body')
                                    .toast({
                                        class: 'error',
                                        message: 'Could not delete file'
                                    })
                                ;
                            }
                        });
                        break;*/
                    // case "new-folder":
                    //     promptFolderCreation();
                    //     break;
                    /* default:
                         console.log(row.getAttribute("data-id"));
                         console.log(link.getAttribute("data-action"))
                         console.log(row);
                         console.log("Task ID - " + row.getAttribute("data-id") + ", Task action - " + link.getAttribute("data-action"));
                         break;*/
                }
            }
        );
        toggleMenuOff();
    }

    /*for (var i = 0; i < getSelectedRows().length; i++) {
        var row = getSelectedRows()[i];
        console.log(row);
        console.log("Task ID - " + row.getAttribute("data-id") + ", Task action - " + link.getAttribute("data-action"));
    }*/

}

/**
 * Run the app.
 */
init();