import os.path

from flask import Flask, flash, request, redirect, url_for
from werkzeug.utils import secure_filename

app = Flask(__name__)

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000)

UPLOAD_FOLDER = 'C:\\Users\\bjast\\PythonProjects\\flask-app\\images'
ALLOWED_EXTENSIONS = {'txt', 'png', 'jpg', 'jpeg'}
FOLDER = "images/"
app.secret_key = 'super secret key'
app.config['SESSION_TYPE'] = 'filesystem'


def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/file_upload', methods=['GET', 'POST'])
def file_upload():
    if request.method == 'POST':
        file = request.files['file']
        PATH = FOLDER+'image.jpg'
        if os.path.exists(PATH):
            os.remove(PATH)
        new_file = open(PATH, "x")

        file.save(os.path.join(UPLOAD_FOLDER, secure_filename(file.filename)))

        print(file)
        new_file.close()
        return 'file received'

    return 'file_upload run'


@app.route('/recognition/<image>', methods=["GET", "POST"])
def recognition(image):
    if request.method == 'POST':
        file = request.files['image']
        if os.path.exists(FOLDER+image):
            os.remove(FOLDER+image)
        new_file = open(FOLDER+image, "x")

        file.save(os.path.join(UPLOAD_FOLDER, secure_filename(file.filename)))

        print(file)
        new_file.close()
        return 'file received'


@app.route('/', methods=['GET', 'POST'])
def upload_file():
    if request.method == 'POST':
        # check if the post request has the file part
        if 'file' not in request.files:
            flash('No file part')
            return redirect(request.url)
        file = request.files['file']
        # If the user does not select a file, the browser submits an
        # empty file without a filename.
        if file.filename == '':
            flash('No selected file')
            return redirect(request.url)
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            file.save(os.path.join(app.instance_path, filename))
            return redirect(url_for('upload_file', name=filename))
    return '''
    <!doctype html>
    <title>Upload new File</title>
    <h1>Upload new File</h1>
    <form method=post enctype=multipart/form-data>
      <input type=file name=file>
      <input type=submit value=Upload>
    </form>
    '''
