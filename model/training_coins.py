from skimage import io,transform
import glob
import os
os.environ['TF_CPP_MIN_LOG_LEVEL']='2'
import tensorflow as tf
import numpy as np
import time
from tensorflow.python.framework import graph_util
import tempfile
import subprocess
tf.contrib.lite.tempfile = tempfile
tf.contrib.lite.subprocess = subprocess



#path of dataset
# path='D:/Download/CoinsDatasets/coins_photo/'
path = './data/coins'
#path to store model
# model_path='D:/Download/CoinsDatasets/model/coins/model.ckpt'
model_path = './data/model.ckpt'

#resize all photos into 100*100 and rgb
w=224
h=224

c=3

#read photos
def read_img(path):
    cate = [path + '/' + x for x in os.listdir(path) if os.path.isdir(path + '/' + x)]
    imgs=[]
    labels=[]
    for idx,folder in enumerate(cate):
        for im in glob.glob(folder+'/*.jp*g'):
            print('reading the images:%s'%(im))
            img=io.imread(im)
            img=transform.resize(img,(w,h))
            imgs.append(img)
            labels.append(idx)
    return np.asarray(imgs,np.float32),np.asarray(labels,np.int32)
data,label=read_img(path)

#dis- order
num_example=data.shape[0]
arr=np.arange(num_example)
np.random.shuffle(arr)
data=data[arr]
label=label[arr]

#divide datasets into training data and test dats
ratio=0.9
s=np.int(num_example*ratio)
x_train=data[:s]
y_train=label[:s]
x_val=data[s:]
y_val=label[s:]

# ----------------------build network----------------------
#place holder
x=tf.placeholder(name='x', dtype=tf.float32, shape=[1,w,h,c])
y_=tf.placeholder(name='y_', dtype= tf.int32, shape=[None,])


def inference(input_tensor, train, regularizer):
    with tf.variable_scope('layer1-conv1'):
        # 5 is input width, 5 is input height
        # 3 is input size and 32 is output size
        conv1_weights = tf.get_variable("weight",[5,5,3,32],initializer=tf.truncated_normal_initializer(stddev = 0.1))
        conv1_biases = tf.get_variable("bias", [32], initializer=tf.constant_initializer(0.0))
        # strides means skip, 1 is not skip any sample
        conv1 = tf.nn.conv2d(input_tensor, conv1_weights, strides=[1, 1, 1, 1], padding='SAME')
        relu1 = tf.nn.relu(tf.nn.bias_add(conv1, conv1_biases))

    with tf.name_scope("layer2-pool1"):
        pool1 = tf.nn.max_pool(relu1, ksize = [1,2,2,1],strides=[1,2,2,1],padding="VALID")

    with tf.variable_scope("layer3-conv2"):
        conv2_weights = tf.get_variable("weight",[5,5,32,64],initializer=tf.truncated_normal_initializer(stddev = 0.1))
        conv2_biases = tf.get_variable("bias", [64], initializer=tf.constant_initializer(0.0))
        conv2 = tf.nn.conv2d(pool1, conv2_weights, strides=[1, 1, 1, 1], padding='SAME')
        relu2 = tf.nn.relu(tf.nn.bias_add(conv2, conv2_biases))

    with tf.name_scope("layer4-pool2"):
        pool2 = tf.nn.max_pool(relu2, ksize=[1, 2, 2, 1], strides=[1, 2, 2, 1], padding='VALID')

    with tf.variable_scope("layer5-conv3"):
        conv3_weights = tf.get_variable("weight",[3,3,64,128],initializer=tf.truncated_normal_initializer(stddev = 0.1))
        conv3_biases = tf.get_variable("bias", [128], initializer=tf.constant_initializer(0.0))
        conv3 = tf.nn.conv2d(pool2, conv3_weights, strides=[1, 1, 1, 1], padding='SAME')
        relu3 = tf.nn.relu(tf.nn.bias_add(conv3, conv3_biases))

    with tf.name_scope("layer6-pool3"):
        pool3 = tf.nn.max_pool(relu3, ksize=[1, 2, 2, 1], strides=[1, 2, 2, 1], padding='VALID')

    with tf.variable_scope("layer7-conv4"):
        conv4_weights = tf.get_variable("weight",[3,3,128,128],initializer=tf.truncated_normal_initializer(stddev = 0.1))
        conv4_biases = tf.get_variable("bias", [128], initializer=tf.constant_initializer(0.0))
        conv4 = tf.nn.conv2d(pool3, conv4_weights, strides=[1, 1, 1, 1], padding='SAME')
        relu4 = tf.nn.relu(tf.nn.bias_add(conv4, conv4_biases))

    with tf.name_scope("layer8-pool4"):
        pool4 = tf.nn.max_pool(relu4, ksize=[1, 2, 2, 1], strides=[1, 2, 2, 1], padding='VALID')
        nodes = 14*14*128
        reshaped = tf.reshape(pool4,[-1,nodes])


    # # ///////////////////////
    # with tf.variable_scope("layer9-conv5"):
    #     conv5_weights = tf.get_variable("weight", [2,2,256,256], initializer=tf.contrib.layers.xavier_initializer())
    #     conv5_biases = tf.get_variable("bias", [256], initializer=tf.constant_initializer(0.0))
    #     conv5 = tf.nn.conv2d(pool4, conv5_weights, strides=[1,1,1,1], padding='SAME')
    #     relu5 = tf.nn.relu(tf.nn.bias_add(conv5, conv5_biases))
    # with tf.name_scope("layer10-pool5"):
    #     pool5 = tf.nn.max_pool(relu5, ksize=[1,2,2,1], strides=[1,2,2,1], padding='VALID')
    #     nodes = 7*7*256
    #     reshaped = tf.reshape(pool5,[-1, nodes])
    # //////////////////////////


    with tf.variable_scope('layer9-fc1'):
        fc1_weights = tf.get_variable("weight", [nodes, 1024],
                                      initializer=tf.truncated_normal_initializer(stddev = 0.1))
        if regularizer != None: tf.add_to_collection('losses', regularizer(fc1_weights))
        fc1_biases = tf.get_variable("bias", [1024], initializer=tf.constant_initializer(0.1))

        fc1 = tf.nn.relu(tf.matmul(reshaped, fc1_weights) + fc1_biases)
        if train: fc1 = tf.nn.dropout(fc1, 0.5)

    with tf.variable_scope('layer10-fc2'):
        fc2_weights = tf.get_variable("weight", [1024, 512],
                                      initializer=tf.truncated_normal_initializer(stddev = 0.1))
        if regularizer != None: tf.add_to_collection('losses', regularizer(fc2_weights))
        fc2_biases = tf.get_variable("bias", [512], initializer=tf.constant_initializer(0.1))

        fc2 = tf.nn.relu(tf.matmul(fc1, fc2_weights) + fc2_biases)
        if train: fc2 = tf.nn.dropout(fc2, 0.5)

    with tf.variable_scope('layer11-fc3'):
        fc3_weights = tf.get_variable("weight", [512, 5],
                                      initializer=tf.truncated_normal_initializer(stddev = 0.1))
        if regularizer != None: tf.add_to_collection('losses', regularizer(fc3_weights))

        fc3_biases = tf.get_variable("bias", [5], initializer=tf.constant_initializer(0.1))
        logit = tf.matmul(fc2, fc3_weights) + fc3_biases


        # fc3_biases = tf.get_variable("bias", [512], initializer=tf.constant_initializer(0.1))
        # fc3 = tf.nn.relu(tf.matmul(fc2, fc3_weights) + fc3_biases)
        # if train: fc3 = tf.nn.dropout(fc3, 0.5)

    # with tf.variable_scope('layer12-fc4'):
    #     fc4_weights = tf.get_variable("weight", [512, 5],
    #                                   initializer=tf.truncated_normal_initializer(stddev = 0.1))
    #     if regularizer != None: tf.add_to_collection('losses', regularizer(fc4_weights))
    #     fc4_biases = tf.get_variable("bias", [5], initializer=tf.constant_initializer(0.1))
    #     logit = tf.matmul(fc3, fc4_weights) + fc4_biases


    return logit

# ---------------network finish-------------------
regularizer = tf.contrib.layers.l2_regularizer(0.0001)
logits = inference(x,False,regularizer)


#(小处理)将logits乘以1赋值给logits_eval，定义name，方便在后续调用模型时通过tensor名字调用输出tensor
b = tf.constant(value=1,dtype=tf.float32)
logits_eval = tf.multiply(logits,b,name='logits_eval')

loss=tf.nn.sparse_softmax_cross_entropy_with_logits(logits=logits, labels=y_)
train_op=tf.train.AdamOptimizer(learning_rate=0.001).minimize(loss)
correct_prediction = tf.equal(tf.cast(tf.argmax(logits,1),tf.int32), y_)
acc= tf.reduce_mean(tf.cast(correct_prediction, tf.float32))


#定义一个函数，按批次取数据
#mini batch：每次随机选择batch_size 个样本，损失函数定义在batch_size 个样本上。每次都是在batch_size个样本上作梯度下降。
def minibatches(inputs=None, targets=None, batch_size=None, shuffle=False):
    assert len(inputs) == len(targets)
    if shuffle:
        indices = np.arange(len(inputs))
        np.random.shuffle(indices)
    for start_idx in range(0, len(inputs) - batch_size + 1, batch_size):
        if shuffle:
            excerpt = indices[start_idx:start_idx + batch_size]
        else:
            excerpt = slice(start_idx, start_idx + batch_size)
        yield inputs[excerpt], targets[excerpt]

#训练和测试数据，可将n_epoch设置更大一些
n_epoch = 8
batch_size=1
saver=tf.train.Saver()
sess=tf.Session()
sess.run(tf.global_variables_initializer())
for epoch in range(n_epoch):
    start_time = time.time()

    # training
    train_loss, train_acc, n_batch = 0, 0, 0
    for x_train_a, y_train_a in minibatches(x_train, y_train, batch_size, shuffle=True):
        _, err, ac = sess.run([train_op, loss, acc], feed_dict={x: x_train_a, y_: y_train_a})
        train_loss += err
        train_acc += ac
        n_batch += 1
    print("   train loss: %f" % (np.sum(train_loss) / n_batch))
    print("   train acc: %f" % (np.sum(train_acc) / n_batch))

    # validation
    val_loss, val_acc, n_batch = 0, 0, 0
    for x_val_a, y_val_a in minibatches(x_val, y_val, batch_size, shuffle=False):
        err, ac = sess.run([loss, acc], feed_dict={x: x_val_a, y_: y_val_a})
        val_loss += err
        val_acc += ac
        n_batch += 1
    print("   validation loss: %f" % (np.sum(val_loss) / n_batch))
    print("   validation acc: %f" % (np.sum(val_acc) / n_batch))
    print("   -----------------------------")

constant_graph = graph_util.convert_variables_to_constants(sess, sess.graph_def, ["logits_eval"])

with tf.gfile.FastGFile('./data/model.pb', mode='wb') as f:
    f.write(constant_graph.SerializeToString())

tflite_model = tf.contrib.lite.toco_convert(constant_graph, [x], [logits_eval]) #这里[input], [out]这里分别是输入tensor或者输出tensor的集合,是变量实体不是名字
open("../app/assets/model.tflite", "wb").write(tflite_model)

# saver.save(sess, model_path)
sess.close()