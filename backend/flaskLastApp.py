from flask import Flask, request, jsonify
import json
import sys
import datetime
import sqlite3
import matplotlib.pyplot as plt
import pytz

app = Flask(__name__)




@app.route('/api/receive_data', methods=['POST'])
def esp32_data():
    try:

            data = request.get_json()
            measure = data['measure']
            user_id = data['user_id']
            unit = data['unit']
            utc_now = datetime.datetime.now(pytz.utc)

            tr_timezone = pytz.timezone('Europe/Istanbul')

            tr_time = utc_now.astimezone(tr_timezone)

            print("Türkiye Saati:", tr_time.strftime('%Y-%m-%d %H:%M:%S'))
            print(measure)
            conn = sqlite3.connect('healthcare.db')
            cursor = conn.cursor()

            cursor.execute("INSERT INTO measurements (user_id,weight,weight_type,date_of_measurement) VALUES (?,?,?,?)",(user_id,measure,unit,tr_time.strftime('%Y-%m-%d %H:%M:%S')))
            conn.commit()
            cursor.close()

            response_data = {'message': 'Received data', 'data': data}
            return jsonify(response_data)   
    except Exception as e:
            return jsonify({"error": str(e)}), 400


@app.route('/api/createacc', methods=['POST','GET'])
def create_acc():
    data = request.get_json()
    username = data['username']
    password = data['password']
    print(data)
    conn = sqlite3.connect('healthcare.db')
    cursor = conn.cursor()

    alper = cursor.execute("INSERT INTO accounts (username,password) VALUES (?,?)", (username,password))
    conn.commit()

    cursor.execute("INSERT INTO users (user_id) VALUES (?)",[username])
    conn.commit()
    cursor.close()
# Process the data and return a response

    response_data = {'message': 'Received data', 'data': data}
    return jsonify(response_data)

@app.route('/api/login', methods=['POST'])
def login():
    try:
        data = request.get_json()
        username = data['username']
        password = data['password']

        conn = sqlite3.connect('healthcare.db')
        cursor = conn.cursor()

        # Check if the provided username and password match a record in the database
        cursor.execute("SELECT * FROM accounts WHERE username=? AND password=?", (username, password))
        user = cursor.fetchone()

        conn.close()

        if user:
            return jsonify({"message": "Login successful"}), 200
        else:
            return jsonify({"error": "Invalid username or password"}), 401

    except Exception as e:
        return jsonify({"error": str(e)}), 400




@app.route('/api/editaccinfo', methods = ['POST','GET'])
def edit_acc_info():
        try:
                data = request.get_json()
                username = data['username']
                name = data['name']
                password = data['password']
                email = data['email']
                print("Received JSON data:", data)
                conn = sqlite3.connect('healthcare.db')
                cursor = conn.cursor()

                cursor.execute("UPDATE users SET name = ?,email = ? WHERE user_id = ?",(name,email,username))
                conn.commit()
                cursor.execute("UPDATE accounts SET password = ? WHERE username = ?",(password,username))
                conn.commit()
                cursor.close()


                print("Received JSON data:", data)
                return jsonify({"message": "Data received successfully"}), 200
        except Exception as e:
                 return jsonify({"error": str(e)}), 402



@app.route('/api/editpersinfo', methods = ['POST','GET'])

def edit_pers_info():
        try:
                data = request.get_json()
                username = data['username']
                age = data['age']
                height = data['height']
                gender = data['gender']
                print("Received JSON data:", data)
                conn = sqlite3.connect('healthcare.db')
                cursor = conn.cursor()

                cursor.execute("UPDATE users SET gender  = ?,age = ?, height = ? WHERE user_id = ?",(gender,age,height,username))
                conn.commit()
                cursor.close()


                print("Received JSON data:", data)
                return jsonify({"message": "Data received successfully"}), 200
        except Exception as e:
                 return jsonify({"error": str(e)}), 402


@app.route('/api/edittargetinfo', methods = ['POST','GET'])
def edit_target_info():
        try:
                data = request.get_json()
                username = data['username']
                targetweight = data['targetweight']
                targetsteps = data['targetsteps']
                print("Received JSON data:", data)
                conn = sqlite3.connect('healthcare.db')
                cursor = conn.cursor()

                cursor.execute("UPDATE users SET targetweight  = ?,targetsteps = ? WHERE user_id = ?",(targetweight,targetsteps,username))
                conn.commit()
                cursor.close()


                print("Received JSON data:", data)
                return jsonify({"message": "Data received successfully"}), 200
        except Exception as e:
                 return jsonify({"error": str(e)}), 402



@app.route('/api/deleteacc', methods = ['POST','GET'])
def delete_acc():
        try:
                data = request.get_json()
                username = data['username']
                print("Received JSON data:", data)
                conn = sqlite3.connect('healthcare.db')
                cursor = conn.cursor()

                cursor.execute("DELETE FROM users WHERE user_id = ?",[username])
                conn.commit()
                cursor.execute("DELETE FROM accounts WHERE username = ?",[username])
                conn.commit()
                cursor.close()


                print("Received JSON data:", data)
                return jsonify({"message": "Data received successfully"}), 200
        except Exception as e:
                 return jsonify({"error": str(e)}), 402

@app.route('/api/get_email', methods=['POST'])
def get_email():
    try:
        data = request.get_json()
        username = data['username']

        conn = sqlite3.connect('healthcare.db')
        cursor = conn.cursor()

        # Retrieve the email corresponding to the provided username
        cursor.execute("""
            SELECT users.email
            FROM users
            INNER JOIN accounts ON users.user_id = accounts.username
            WHERE accounts.username = ?
        """, (username,))
        
        email = cursor.fetchone()

        conn.close()

        if email:
            return jsonify({"email": email[0]}), 200
        else:
            return jsonify({"error": "Username not found"}), 404

    except Exception as e:
        return jsonify({"error": str(e)}), 400


@app.route('/api/get_user_info', methods=['POST'])
def get_user_info():
    try:
        data = request.get_json()
        username = data['username']

        conn = sqlite3.connect('healthcare.db')
        cursor = conn.cursor()

        # Retrieve age, gender, and height based on the provided username
        cursor.execute("""
            SELECT age, gender, height, targetweight, targetsteps
            FROM USERS
            INNER JOIN accounts on users.user_id = accounts.username
            WHERE accounts.username = ?
        """, (username,))
        
        user_info = cursor.fetchone()

        conn.close()

        if user_info:
            age, gender, height, targetweight, targetsteps  = user_info  # Unpack the tuple
            user_info_dict = {
                "age": age,
                "gender": gender,
                "height": height,
                "targetweight" : targetweight,
                "targetsteps": targetsteps
            }
            return jsonify(user_info_dict), 200
        else:
            return jsonify({"error": "User information not found"}), 404

    except Exception as e:
        return jsonify({"error": str(e)}), 400


@app.route('/api/bmi', methods=['POST'])
def bmi():
        data = request.get_json()
        username = data['username']

        conn = sqlite3.connect('healthcare.db')  # Replace with your database file path
        cursor = conn.cursor()
        


        cursor.execute("UPDATE measurements SET user_id = ?",[username])
        conn.commit()

        cursor.execute('SELECT weight,height FROM measurements INNER JOIN users ON measurements.user_id = users.user_id WHERE measurements.user_id = ?  ORDER BY date_of_measurement DESC LIMIT 1',[username])
        newdata=cursor.fetchone()

        conn.close()


        integer_values = [int(item) for item in newdata]
        strheight = str(integer_values[1])
        position = 0
        modified_string = strheight[:1] + "." + strheight[position + 1:]
        float_value = float(modified_string)
        print(integer_values[0])
        print(float_value)
        bmi =  integer_values[0]/(float_value*float_value)
        print(bmi)

        if(bmi<18.5):
                return jsonify({"message":"Underweight"}),200
        elif(18.5<=bmi<25):
                return jsonify({"message":"Healthy Weight"}),200
        elif(25<=bmi<30):
                return jsonify({"message":"Overweight"}),200
        else:
                return jsonify({"message":"Obesity"}),200

#        return jsonify({"message": "Data received successfully"}), 200



@app.route('/api/get_weight', methods = ['POST'])
def get_weight():
        try:
                data = request.get_json()
                username = data['username']
                print("Received JSON data:", data)
                conn = sqlite3.connect('healthcare.db')
                cursor = conn.cursor()

                cursor.execute("UPDATE measurements SET user_id = ?",[username])
                conn.commit()
                cursor.execute("SELECT date_of_measurement,weight FROM measurements  ORDER BY date_of_measurement DESC LIMIT 5")
                newdata=cursor.fetchall()
                print(newdata)
                cursor.close()

                veri_listesi = []
                for veri in newdata:
                        veri_dict = {
                                'DATE': veri[0],
                                'Weight': veri[1]
                        }
                        veri_listesi.append(veri_dict)

                print("Received JSON data:", data)
                print(veri_listesi)
                return jsonify({'message':veri_listesi}), 200
        except Exception as e:
                 return jsonify({"error": str(e)}), 402



if __name__ == '__main__':
    app.run(host='0.0.0.0', port=80, debug=True)

